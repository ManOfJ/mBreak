package com.manofj.minecraft.moj_mbreak

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.google.common.collect.Lists
import com.google.common.collect.Maps

import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks.TORCH
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumActionResult.SUCCESS
import net.minecraft.util.EnumFacing.EAST
import net.minecraft.util.EnumFacing.NORTH
import net.minecraft.util.EnumFacing.SOUTH
import net.minecraft.util.EnumFacing.WEST
import net.minecraft.util.EnumHand.MAIN_HAND
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.Style
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World

import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed
import net.minecraftforge.event.world.BlockEvent.BreakEvent
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent


object mBreakEventHandler {
  import math.ceil
  import math.floor

  import net.minecraft.client.resources.I18n.{ format => l10n }

  import com.manofj.minecraft.moj_mbreak.mBreakSettings.{ Local => localSettings }
  import com.manofj.minecraft.moj_mbreak.{ mBreakSettings => globalSettings }


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
  private[ this ] val torchItemStack = new ItemStack( TORCH )


  @SubscribeEvent
  def keyInput( event: KeyInputEvent ): Unit = {
    def stateText( enabled: Boolean ): String =
      if ( enabled ) l10n( "moj_mbreak.state.enabled" ) else l10n( "moj_mbreak.state.disabled" )

    def printChatMessage( message: String ): Unit =
      FMLClientHandler.instance.getClient.ingameGUI.getChatGUI.printChatMessage {
        new TextComponentString( "" ).appendSibling( new TextComponentString( "[mBreak]: " ).setStyle( new Style().setBold( true ) ) ).appendText( message )
      }

    if ( globalSettings.miningSpeedMultToggleKey.isKeyDown ) {
      localSettings.miningSpeedMult = !localSettings.miningSpeedMult
      printChatMessage( l10n( "moj_mbreak.chat.mining_speedmult", stateText( localSettings.miningSpeedMult ) ) )
    }
    if ( globalSettings.chainDestructionToggleKey.isKeyDown ) {
      localSettings.chainDestruction = !localSettings.chainDestruction
      printChatMessage( l10n( "moj_mbreak.chat.chain_destruction", stateText( localSettings.chainDestruction ) ) )
    }
    if ( globalSettings.torchAutoPlacementToggleKey.isKeyDown ) {
      localSettings.torchAutoPlacement = !localSettings.torchAutoPlacement
      printChatMessage( l10n( "moj_mbreak.chat.torch_auto_placement",
                        stateText( localSettings.torchAutoPlacement ) ) )
    }
  }

  @SubscribeEvent
  def breakSpeedEvent( event: BreakSpeed ): Unit = {
    val player = event.getEntityPlayer
    val canToolHarvestBlock = ForgeHooks.canToolHarvestBlock( player.worldObj, event.getPos, player.getHeldItemMainhand )

    if ( localSettings.miningSpeedMult && canToolHarvestBlock )
      event.setNewSpeed( ( event.getOriginalSpeed * globalSettings.miningSpeedMult ).toFloat )

  }


  @SubscribeEvent
  def blockBreak( evt: BreakEvent ): Unit = {
    // プレイヤーの目線の高さをブロック座標で取得する関数
    def getEyeHeightY( plyr: EntityPlayer ): Int =
      floor( plyr.posY + ceil( plyr.getEyeHeight ) - 0.5F ).toInt

    // ブロックを即座に破壊する関数
    def breakBelowBlock( wld: World, pos: BlockPos, plyr: EntityPlayerMP ): Unit = {
      if (    !wld.isAirBlock( pos )
           && ForgeHooks.canToolHarvestBlock( wld, pos, plyr.getHeldItemMainhand ) )
      {
        positions.add( pos )
        wld.sendBlockBreakProgress( plyr.getEntityId, pos, -1 )
        plyr.interactionManager.tryHarvestBlock( pos )
      }
    }

    // たいまつ自動設置処理を実行できる条件がそろっているか評価する関数
    def tAutoPlacementCheckCondition( plyr:  EntityPlayerMP,
                                      pos:   BlockPos,
                                      state: IBlockState ): Boolean =
      localSettings.torchAutoPlacement  &&
      globalSettings.torchAutoPlacement &&
      !positions.contains( pos )        &&
      state.getBlock != TORCH           &&
      ForgeHooks.canToolHarvestBlock( plyr.worldObj, pos, plyr.getHeldItemMainhand )


    // 座標の明るさレベルが 7 以下であるか評価する関数
    def isDarker( wld: World, pos: BlockPos ): Boolean =
      wld.getLightFromNeighbors( pos ) <= 7


    evt.getPlayer match {
      case player: EntityPlayerMP => // サーバー側の処理
        import evt._
        if ( tAutoPlacementCheckCondition( player, getPos, getState ) ) // たいまつ自動設置処理
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

              val hitPos = getPos.add( 0.5D, 0.5D, 0.5D )
              // たいまつの設置処理を行うアクター
              val tPlacementActor = system.actorOf {
                Props { new Actor {
                  // 実行回数カウンタ
                  private[ this ] val counter = Stream.from( 1 ).iterator

                  // たいまつの設置を試みる関数
                  private[ this ] def torchPlacement() =
                    torchItemStack.onItemUse(
                      player,
                      getWorld,
                      getPos,
                      MAIN_HAND,
                      side,
                      hitPos.getX,
                      hitPos.getY,
                      hitPos.getZ
                    ) == SUCCESS

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
                      if ( player.inventory.hasItemStack( torchItemStack ) ) {
                        // 指定座標が空気ブロックで､なおかつ明るさレベルが 7 以下の場合
                        // プレイヤーのインベントリからたいまつを使用する
                        if ( getWorld.isAirBlock( getPos ) && isDarker( getWorld, getPos ) ) {
                          torchItemStack.stackSize = 64
                          if ( torchPlacement() ) {
                            val slot = player.inventory.getSlotFor( torchItemStack )
                            player.inventory.decrStackSize( slot, 1 )
                            if ( !player.inventory.hasItemStack( torchItemStack ) )
                              player.addChatMessage { new TextComponentTranslation(
                                l10n( "moj_mbreak.chat.torch_runout" )
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
              mBreak.warn(
                "Illegal EnumFacing from EntityPlayerMP#getHorizontalFacing" )
          }

        }
        if ( localSettings.chainDestruction && globalSettings.chainDestruction ) // 連鎖破壊処理
        {
          if ( positions contains getPos ) // 連鎖破壊により壊されたブロックの場合
          {
            // ブロックのY座標がプレイヤーのY座標より高ければ､さらに下のブロックを破壊する
            if ( getPos.getY > player.posY )
              breakBelowBlock( getWorld, getPos.down, player )

            positions remove getPos
          }
          else // 正規の手段で壊されたブロックの場合
          {
            // ブロックの高さがプレイヤーの目線の高さと同じなら､下のブロックを破壊する
            if ( getPos.getY == getEyeHeightY( player ) )
              breakBelowBlock( getWorld, getPos.down, player )
          }
        }
      case _ =>
        // クライアント側では何もしない
    }
  }

}
