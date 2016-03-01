package com.manofj.minecraft.moj_mbreak

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.I18n

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent


object mBreakKeyHandler {

  // 採掘高速化トグルキーが押下状態にあるか否か
  private[ this ] var toggle_mining_speedmult_pressed = false

  // 連鎖破壊トグルキーが押下状態にあるか否か
  private[ this ] var toggle_chain_destruction_pressed = false

  // たいまつ自動設置トグルキーが押下状態にあるか否か
  private[ this ] var toggle_torch_auto_placement_pressed = false


  /**
    * キー入力がなされたときのイベントをフックする
    * トグルキーの挙動を実装する
    * @param evt キー入力イベント
    */
  @SubscribeEvent
  def keyInput( evt: KeyInputEvent ): Unit = {
    import org.lwjgl.input.Keyboard

    import mBreakConfigHandler.{ toggle_chain_destruction, toggle_mining_speedmult, toggle_torch_auto_placement }
    import mBreakState.{ enable_chain_destruction, enable_mining_speedmult, enable_torch_auto_placement }

    // 通知メッセージの表示
    def systemMessage( message: String, enabled: Boolean ): Unit = {
      val twostate = if ( enabled ) "enabled" else "disabled"
      val state = I18n.format( s"moj_mbreak.chat.toggle.$twostate" )
      Minecraft.getMinecraft.thePlayer.sendChatMessage( I18n.format( message, state ) )
    }

    toggle_mining_speedmult.foreach {
      case ( primaryKey, combinations )
        if primaryKey.isPressed && combinations.forall( Keyboard.isKeyDown ) =>

          if ( !toggle_mining_speedmult_pressed ) {
            enable_mining_speedmult = !enable_mining_speedmult
            systemMessage( "moj_mbreak.chat.toggle_mining_speedmult", enable_mining_speedmult )
          }

          toggle_mining_speedmult_pressed = true
      case _ =>
        toggle_mining_speedmult_pressed = false
    }

    toggle_chain_destruction.foreach {
      case ( primaryKey, combinations )
        if primaryKey.isPressed && combinations.forall( Keyboard.isKeyDown ) =>

          if ( !toggle_chain_destruction_pressed ) {
            enable_chain_destruction = !enable_chain_destruction
            systemMessage( "moj_mbreak.chat.toggle_chain_destruction", enable_chain_destruction )
          }

          toggle_chain_destruction_pressed = true
      case _ =>
        toggle_chain_destruction_pressed = false
    }

    toggle_torch_auto_placement.foreach {
      case ( primaryKey, combinations )
        if primaryKey.isPressed && combinations.forall( Keyboard.isKeyDown ) =>

          if ( !toggle_torch_auto_placement_pressed ) {
            enable_torch_auto_placement = !enable_torch_auto_placement
            systemMessage( "moj_mbreak.chat.toggle_torch_auto_placement", enable_torch_auto_placement )
          }

          toggle_torch_auto_placement_pressed = true
      case _ =>
        toggle_torch_auto_placement_pressed = false
    }

  }

}
