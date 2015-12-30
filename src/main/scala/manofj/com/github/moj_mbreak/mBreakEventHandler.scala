package manofj.com.github.moj_mbreak

import akka.actor.{ Actor, ActorSystem, Props }
import com.google.common.collect.Lists
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.{ EntityPlayer, EntityPlayerMP }
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing.{ NORTH, WEST, SOUTH, EAST }
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
  import mBreakConfigHandler.{ mining_speedmult, chain_destruction, torch_auto_placement }

  // たいまつ設置アクターの ID
  private[ this ] final val TORCH_PLACEMENT = "actor:torch_placement"

  // 連鎖破壊により壊されたブロックの座標リスト
  private[ this ] val positions = Lists.newArrayList[ BlockPos ]

  // アクターシステム
  private[ this ] val system = ActorSystem( "system" )

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
      floor( plyr.posY + ceil( plyr.getEyeHeight ) - 0.5F ) toInt

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
    def tAutoPlacementCheckCondition( pos: BlockPos, state: IBlockState ): Boolean =
      torch_auto_placement && !positions.contains( pos ) && state.getBlock != Blocks.torch

    // 座標の明るさレベルが 7 以下であるか評価する関数
    def isDarker( wld: World, pos: BlockPos ): Boolean =
      wld.getLightFromNeighbors( pos ) <= 7


    evt.getPlayer match {
      case player: EntityPlayerMP => // サーバー側の処理
        import evt._
        if ( tAutoPlacementCheckCondition( pos, state ) ) { // たいまつ自動設置処理
          val sideOpt = player.getHorizontalFacing match {
            case NORTH | SOUTH  => Option( WEST )
            case WEST  | EAST   => Option( NORTH )
            case _              => None
          }

          sideOpt match {
            case Some( side ) =>
              import system.dispatcher, scala.concurrent.duration._

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
                    case TORCH_PLACEMENT =>
                      // プレイヤーがたいまつを所持していることが前提条件
                      if ( player.inventory.hasItem( torchItemStack.getItem ) ) {
                        // 指定座標が空気ブロックで､なおかつ明るさレベルが 7 以下の場合
                        // プレイヤーのインベントリからたいまつを使用する
                        if ( world.isAirBlock( pos ) && isDarker( world, pos ) ) {
                          torchItemStack.stackSize = 64
                          if ( torchPlacement() ) {
                            player.inventory.consumeInventoryItem( torchItemStack.getItem )
                            // 処理が完了したためカウンタを進める
                            counter.dropWhile( _ < 5 )
                          }
                        }
                      }
                      // 5 回実行したら自らを破棄する
                      if ( counter.next >= 5 ) system.stop( self )
                  }
                }}
              }
              // アクターのスケジューラにたいまつ設置アクターを設定する
              system.scheduler.schedule( 0 millisecond, 100 millisecond, tPlacementActor, TORCH_PLACEMENT )
            case None         =>
              mBreak.log.warn(
                "Illegal EnumFacing from EntityPlayerMP#getHorizontalFacing" )
          }

        }
        if ( chain_destruction ) { // 連鎖破壊処理
          if ( positions contains pos ) { // 連鎖破壊により壊されたブロックの場合
            // ブロックのY座標がプレイヤーのY座標より高ければ､さらに下のブロックを破壊する
            if ( pos.getY > player.posY )
              breakBelowBlock( world, pos.down, player )

            positions remove pos
          }
          else { // 正規の手段で壊されたブロックの場合
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
