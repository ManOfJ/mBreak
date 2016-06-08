package com.manofj.minecraft.moj_mbreak

import org.apache.logging.log4j.{ LogManager, Logger }

import net.minecraft.client.settings.KeyBinding
import net.minecraft.launchwrapper.Launch

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{ FMLInitializationEvent, FMLPreInitializationEvent }

/**
  * mBreak の Mod オブジェクト
  * 主に各種ハンドラの初期化、レジストリへの登録を行う
  */
@Mod( modid       = MOD_ID,
      name        = NAME,
      version     = VERSION,
      updateJSON  = UPDATE_JSON,
      modLanguage = LANGUAGE,
      guiFactory  = GUI_FACTORY )
object mBreak {
  // オブジェクトの初期化時に akka のパッケージをトランスフォーム対象外に
  Launch.classLoader.addTransformerExclusion( "akka." )


  private[this] var logger = Option.empty[ Logger ]


  // ロガーのゲッタ
  def log = logger getOrElse LogManager.getLogger( MOD_ID )


  /**
    * 事前初期化イベント
    * コンフィグの読み込みや、各種ハンドラをレジストリに登録したりする
    * @param evt 事前初期化イベント
    */
  @EventHandler
  def preInit( evt: FMLPreInitializationEvent ): Unit = {
    import com.manofj.minecraft.moj_commons.config.javaFile2ForgeConfig

    logger = Option( evt.getModLog )

    mBreakConfigHandler.captureConfig( evt.getSuggestedConfigurationFile )

    MinecraftForge.EVENT_BUS.register( mBreakEventHandler )
    MinecraftForge.EVENT_BUS.register( mBreakConfigHandler )
  }

  /**
    * 初期化イベント
    * キーバインドの設定を行う
    * @param evt 初期化イベント
    */
  @EventHandler
  def init( evt: FMLInitializationEvent ): Unit = {
    import net.minecraftforge.fml.relauncher.Side.{CLIENT, SERVER}

    evt.getSide match {
      case SERVER => // サーバー側では何もしない
      case CLIENT =>
        def registerKeyBinding( opt: Option[ ( KeyBinding, _ ) ] ): Unit =
          opt.map( _._1 ).foreach( ClientRegistry.registerKeyBinding )

        registerKeyBinding( mBreakConfigHandler.toggle_mining_speedmult )
        registerKeyBinding( mBreakConfigHandler.toggle_chain_destruction )
        registerKeyBinding( mBreakConfigHandler.toggle_torch_auto_placement )
    }

    MinecraftForge.EVENT_BUS.register( mBreakKeyHandler )
  }
}
