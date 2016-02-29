package com.manofj.minecraft.moj_mbreak

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }

import com.google.common.collect.{ Lists, Maps }

import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.{ EntityPlayer, EntityPlayerMP }
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing.{ EAST, NORTH, SOUTH, WEST }
import net.minecraft.util.{ BlockPos, ChatComponentTranslation }
import net.minecraft.world.World

import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed
import net.minecraftforge.event.world.BlockEvent.BreakEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


/**
  * Mod のイベントハンドラ
  * この Mod の中枢部分といえるオブジェクト
  */
object mBreakEventHandler {
  import math.{ ceil, floor }

  import mBreakConfigHandler.{ chain_destruction, mining_speedmult, torch_auto_placement }

  // アクター: たいまつ設置処理を行うメッセージ
  private[ this ] case object TorchPlacement

  // アクター: 自らを破棄するメッセージ
  private[ this ] case object Kill


  // 連鎖破壊により壊されたブロックの座標リスト
  private[ this ] val positions = Lists.newArrayList[ BlockPos ]

  // アクターシステム
  private[ this ] val system = ActorSystem( "system" )

  // プレイヤーに紐づけられたアクティブなたいまつ設置アクター
  private[ this ] val activeActor = Maps.newHashMap[ EntityPlayer, ActorRef ]

  // たいまつのアイテムスタック
  private[ this ] val torchItemStack = new ItemStack( Blocks.torch )

  /**
    * ブロックの採掘速度計算イベントをフックする
    * コンフィグの設定を用いて新たな採掘速度を算出する
    * @param evt 採掘速度設定イベント
    */
  @SubscribeEvent
  def breakSpeedEvent( evt: BreakSpeed ): Unit = {
    // プレイヤーが指定座標のブロックをハーベストできるかどうか評価する
    def canToolHarvestBlock( pos: BlockPos, plyr: EntityPlayer ): Boolean =
      ForgeHooks.canToolHarvestBlock( plyr.worldObj, pos, plyr.getCurrentEquippedItem )

    import evt._
    if ( canToolHarvestBlock( pos, entityPlayer ) ) {
      newSpeed = originalSpeed * mining_speedmult
    }
  }

  /**
    * ブロックが破壊されたときのイベントをフックする
    * 主に連鎖破壊、たいまつの自動設置などの処理を行う
    * @param evt ブロック破壊イベント
    */
  @SubscribeEvent
  def blockBreak( evt: BreakEvent ): Unit = {
    // プレイヤーの目線の高さをブロック座標で取得する関数
    def getEyeHeightY( plyr: EntityPlayer ): Int =
      floor( plyr.posY + ceil( plyr.getEyeHeight ) - 0.5F ).toInt

    // ブロックを即座に破壊する関数
    def breakBelowBlock( wld: World, pos: BlockPos, plyr: EntityPlayerMP ): Unit = {
      if (    !wld.getBlockState( pos ).getBlock.isAir( wld, pos )
           && ForgeHooks.canToolHarvestBlock( wld, pos, plyr.getCurrentEquippedItem ) )
      {
        positions.add( pos )
        wld.sendBlockBreakProgress( plyr.getEntityId, pos, -1 )
        plyr.theItemInWorldManager.tryHarvestBlock( pos )
      }
    }

    // たいまつ自動設置処理を実行できる条件がそろっているか評価する関数
    def tAutoPlacementCheckCondition( plyr:  EntityPlayerMP,
                                      pos:   BlockPos,
                                      state: IBlockState ): Boolean =
      torch_auto_placement           &&
      !positions.contains( pos )     &&
      state.getBlock != Blocks.torch &&
      ForgeHooks.canToolHarvestBlock( plyr.worldObj, pos, plyr.getCurrentEquippedItem )


    // 座標の明るさレベルが 7 以下であるか評価する関数
    def isDarker( wld: World, pos: BlockPos ): Boolean =
      wld.getLightFromNeighbors( pos ) <= 7


    evt.getPlayer match {
      case player: EntityPlayerMP => // サーバー側の処理
        import evt._
        if ( tAutoPlacementCheckCondition( player, pos, state ) ) // たいまつ自動設置処理
        {
          val sideOpt = player.getHorizontalFacing match {
            case NORTH | SOUTH  => Option( WEST )
            case WEST  | EAST   => Option( NORTH )
            case _              => None
          }

          sideOpt match {
            case Some( side ) =>
              import scala.concurrent.duration._

              import system.dispatcher

              val hitPos = pos.add( 0.5D, 0.5D, 0.5D )
              // たいまつの設置処理を行うアクター
              val tPlacementActor = system.actorOf {
                Props { new Actor {
                  // 実行回数カウンタ
                  private[ this ] val counter = Stream.from( 1 ).iterator

                  // たいまつの設置を試みる関数
                  private[ this ] def torchPlacement() =
                    torchItemStack.onItemUse( player, world, pos, side, hitPos.getX, hitPos.getY, hitPos.getZ )


                  // アクター処理内容
                  override def receive: Receive = {
                    case Kill => counter.next match {
                      case 1 => // たいまつ設置処理を行っていなければ一度だけ､即座に実行する
                        counter.dropWhile( _ < 5 )
                        self ! TorchPlacement
                      case _ => system.stop( self )
                    }
                    case TorchPlacement =>
                      // プレイヤーがたいまつを所持していることが前提条件
                      if ( player.inventory.hasItem( torchItemStack.getItem ) ) {
                        // 指定座標が空気ブロックで､なおかつ明るさレベルが 7 以下の場合
                        // プレイヤーのインベントリからたいまつを使用する
                        if ( world.isAirBlock( pos ) && isDarker( world, pos ) ) {
                          torchItemStack.stackSize = 64
                          if ( torchPlacement() ) {
                            player.inventory.consumeInventoryItem( torchItemStack.getItem )
                            if ( !player.inventory.hasItem( torchItemStack.getItem ) )
                              player.addChatMessage { new ChatComponentTranslation(
                                "moj_mbreak.chat.torch_runout",
                                torchItemStack.getDisplayName
                              ) }

                            // 処理が完了したためカウンタを進める
                            counter.dropWhile( _ < 5 )
                          }
                        }
                      }
                      // 3 回実行したら自らを破棄する
                      if ( counter.next >= 3 ) self ! Kill
                  }
                }}
              }

              // アクターのスケジューラにたいまつ設置アクターを設定する
              system.scheduler.schedule( 250.millisecond, 250.millisecond, tPlacementActor, TorchPlacement )

              // たいまつ設置アクターをプレイヤーに紐づける
              Option( activeActor.put( player, tPlacementActor ) ) match {
                // まだ稼働している古いたいまつ設置アクターを破棄する
                case Some( actor ) => actor ! Kill
                case None => // 何もしない
              }
            case None         =>
              mBreak.log.warn(
                "Illegal EnumFacing from EntityPlayerMP#getHorizontalFacing" )
          }

        }
        if ( chain_destruction ) // 連鎖破壊処理
        {
          if ( positions contains pos ) // 連鎖破壊により壊されたブロックの場合
          {
            // ブロックのY座標がプレイヤーのY座標より高ければ､さらに下のブロックを破壊する
            if ( pos.getY > player.posY )
              breakBelowBlock( world, pos.down, player )

            positions remove pos
          }
          else // 正規の手段で壊されたブロックの場合
          {
            // ブロックの高さがプレイヤーの目線の高さと同じなら､下のブロックを破壊する
            if ( pos.getY == getEyeHeightY( player ) )
              breakBelowBlock( world, pos.down, player )
          }
        }
      case _ =>
        // クライアント側では何もしない
    }
  }

}
