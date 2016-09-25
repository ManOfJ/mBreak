package com.manofj.minecraft.moj_mbreak

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.google.common.collect.Lists
import com.google.common.collect.Maps

import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
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

import com.manofj.commons.scala.util.conversions.Any$
import com.manofj.commons.scala.util.conversions.Boolean$


object mBreakEventHandler {
  import net.minecraft.init.Blocks.TORCH
  import net.minecraft.util.EnumFacing.EAST
  import net.minecraft.util.EnumFacing.NORTH
  import net.minecraft.util.EnumFacing.SOUTH
  import net.minecraft.util.EnumFacing.WEST

  import mBreak.format
  import mBreakConfig._


  // アクター: たいまつ設置処理を行うメッセージ
  private[ this ] case object Placement

  // アクター: 自らを破棄するメッセージ
  private[ this ] case object Kill


  private[ this ] val actorSystem = ActorSystem( "system" )

  private[ this ] val torchItem = new ItemStack( TORCH )


  // 連鎖破壊により壊されたブロックの座標リスト
  private[ this ] val positions = Lists.newArrayList[ BlockPos ]

  // プレイヤーに紐づけられたアクティブなたいまつ設置アクター
  private[ this ] val actors = Maps.newHashMap[ UUID, ActorRef ]


  // キー入力による各機能のオン/オフを行う
  @SubscribeEvent
  def keyInput( event: KeyInputEvent ): Unit = {
    def printChatMsg( m: String ) = {
      val chatGui = FMLClientHandler.instance.getClient.ingameGUI.getChatGUI
      chatGui.printChatMessage( chatMsg( m ) )
    }
    def chatMsg( k: String ) = {
      val prefix = new TextComponentString( "[mBreak]: " ).setStyle( new Style().setBold( true ) )
      new TextComponentString( "" ).appendSibling( prefix ).appendText( k )
    }
    def state( f: Boolean ) = format( f ? "state.enabled" ! "state.disabled" )


    if ( miningSpeedMultToggleKey.isKeyDown )
    {
      miningSpeedmultEnable = !miningSpeedmultEnable
      printChatMsg( format( "chat.mining_speedmult", state( miningSpeedmultEnable ) ) )
    }

    if ( chainDestructionToggleKey.isKeyDown )
    {
      chainDestruction = !chainDestruction
      printChatMsg( format( "chat.chain_destruction", state( chainDestruction ) ) )
    }

    if ( torchAutoPlacementToggleKey.isKeyDown )
    {
      torchAutoPlacement = !torchAutoPlacement
      printChatMsg( format( "chat.torch_auto_placement", state( torchAutoPlacement ) ) )
    }

    if ( torchPlacementSideToggleKey.isKeyDown )
    {
      val toggledSide = ( torchPlacementSide == "left" ) ? "right" ! "left"
      torchPlacementSide = toggledSide
      printChatMsg( format( "chat.torch_placement_side", format( s"state.$toggledSide" ) ) )
    }
  }

  // ブロック破壊速度を変更する
  @SubscribeEvent
  def breakSpeedEvent( event: BreakSpeed ): Unit =
    if ( miningSpeedmultEnable && miningSpeedmult > 0 )
    {
      val player = event.getEntityPlayer
      if ( ForgeHooks.canToolHarvestBlock( player.worldObj, event.getPos, player.getHeldItemMainhand ) )
        event.setNewSpeed( ( event.getOriginalSpeed * miningSpeedmult ).toFloat )
    }

  // 松明の自動設置やブロックの連鎖破壊を行う
  @SubscribeEvent
  def blockBreak( event: BreakEvent ): Unit = {
    import math.ceil
    import math.floor
    import scala.concurrent.duration._

    import actorSystem.dispatcher


    def blockDestroy( w: World, b: BlockPos, p: EntityPlayerMP ): Unit = {
      if ( !w.isAirBlock( b ) &&
           ForgeHooks.canToolHarvestBlock( w, b, p.getHeldItemMainhand ) )
      {
        positions.add( b )
        w.sendBlockBreakProgress( p.getEntityId, b, -1 )
        p.interactionManager.tryHarvestBlock( b )
      }
    }

    def eyeHeightY( p: EntityPlayer ) = floor( p.posY + ceil( p.getEyeHeight ) - 0.5F ).toInt

    def canAutoPlacement( p: EntityPlayerMP, b: BlockPos, s: IBlockState ) =
      torchAutoPlacement &&
      b.getY == eyeHeightY( p ) &&
      s.getBlock != TORCH &&
      ForgeHooks.canToolHarvestBlock( p.worldObj, b, p.getHeldItemMainhand )

    def isDarker( w: World, b: BlockPos ) = w.getLightFromNeighbors( b ) <= 7


    val world = event.getWorld
    val pos   = event.getPos
    ( !world.isRemote, event.getPlayer ) match {
      case ( true, player: EntityPlayerMP ) =>
        // 松明の自動設置処理
        if ( canAutoPlacement( player, pos, event.getState ) )
        {
          val placeLeft = torchPlacementSide == "left"

          var side = player.getHorizontalFacing match {
            case NORTH => placeLeft ? EAST ! WEST
            case SOUTH => placeLeft ? WEST ! EAST
            case WEST  => placeLeft ? NORTH ! SOUTH
            case EAST  => placeLeft ? SOUTH ! NORTH
          }
          var hit  = pos.offset( side.getOpposite )

          // 指定されたサイドに松明を設置できそうになければ
          // 逆サイドへの変更を試みる
          if ( !world.isSideSolid( hit, side ) )
          {
            val oppositePos = pos.offset( side )
            val oppositeSide = side.getOpposite
            if ( world.isSideSolid( oppositePos, oppositeSide ) )
            {
              side = oppositeSide
              hit  = oppositePos
            }
          }

          val uuid = player.getUniqueID
          val actor = actorSystem.actorOf { Props { new Actor {
            private[ this ] val counter = Range( 0, 5 ).iterator
            private[ this ] def placement = torchItem.onItemUse(
              player,
              world,
              pos,
              EnumHand.MAIN_HAND,
              side,
              hit.getX,
              hit.getY,
              hit.getZ
            )

            override def receive: Receive = {
              case Kill =>
                if ( actors.containsValue( self ) ) actors.remove( uuid )
                counter.contains( 0 ) ? ( self ! Placement ) ! actorSystem.stop( self )

              case Placement =>
                val inventory = player.inventory
                if ( !inventory.hasItemStack( torchItem ) ) self ! Kill
                else if ( !counter.hasNext ) self ! Kill
                else
                {
                  counter.next()
                  if ( world.isAirBlock( pos ) && isDarker( world, pos ) )
                  {
                    torchItem.stackSize = 64
                    if ( placement == EnumActionResult.SUCCESS )
                    {
                      inventory.decrStackSize( inventory.getSlotFor( torchItem ), 1 )
                      if ( !inventory.hasItemStack( torchItem ) )
                      {
                        val msg = format( "chat.torch_runout" )
                        player.addChatMessage( new TextComponentTranslation( msg ) )
                      }
                      self ! Kill
                    }
                  }
                }
            }
          } } }

          actorSystem.scheduler.schedule( 200.millisecond, 200.millisecond, actor, Placement )

          actors.get( uuid ).?.foreach( _ ! Kill )
          actors.put( uuid, actor )
        }

        // 連鎖破壊処理
        if ( chainDestruction )
        {
          if ( positions.contains( pos ) )
          {
            if ( pos.getY > player.posY )
            {
              blockDestroy( world, pos.down, player )
            }

            positions.remove( pos )
          }
          else if ( pos.getY == eyeHeightY( player ) )
          {
              blockDestroy( world, pos.down, player )
          }
        }

      case _ => // 何もしない
    }
  }

}
