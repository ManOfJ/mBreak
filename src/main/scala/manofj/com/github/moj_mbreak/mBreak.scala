package manofj.com.github.moj_mbreak

import java.io.File

import net.minecraft.launchwrapper.Launch
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.config.Configuration
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.{ FMLCommonHandler, Mod }
import org.apache.logging.log4j.{ LogManager, Logger }

/**
  * mBreak の Mod オブジェクト
  * 主に各種ハンドラの初期化、レジストリへの登録を行う
  */
@Mod( modid       = MOD_ID,
      name        = NAME,
      version     = VERSION,
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
    logger = Option( evt.getModLog )

    mBreakConfigHandler.captureConfig {
      val cfgDir = evt.getModConfigurationDirectory
      new Configuration( new File( cfgDir, s"$NAME.cfg" ) )
    }

    MinecraftForge.EVENT_BUS.register( mBreakEventHandler )
    FMLCommonHandler.instance.bus.register( mBreakConfigHandler )
  }
}
