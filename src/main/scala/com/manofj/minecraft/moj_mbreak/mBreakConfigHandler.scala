package com.manofj.minecraft.moj_mbreak

import java.util.Collections

import net.minecraft.client.settings.KeyBinding

import com.google.common.collect.Lists

import net.minecraft.client.resources.I18n.{ format => i18n }

import net.minecraftforge.common.config.{ ConfigElement, Configuration, Property }
import net.minecraftforge.fml.client.config.IConfigElement
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


/**
  * Mod のコンフィグハンドラ
  * コンフィグの読み込みや、変数へのバインドなどを行う
  */
object mBreakConfigHandler {
  // このハンドラが対応するコンフィグの ID
  final val CONFIG_ID = Configuration.CATEGORY_GENERAL


  // このハンドラにバインドされているコンフィグオブジェクト
  private[ this ] var config = Option.empty[ Configuration ]

  // 採掘速度の乗数
  private[ this ] var mSpeedmult = 1.0F

  // 連鎖破壊を行うか否か
  private[ this ] var cDestruction = false

  // たいまつの自動設置を行うか否か
  private[ this ] var tAutoPlacement = false

  // 採掘高速化機能のトグルキー
  private[ this ] var mSpeedmultKey = Array.empty[ String ]

  // 連鎖破壊機能のトグルキー
  private[ this ] var cDestructionKey = Array.empty[ String ]

  // たいまつ自動設置機能のトグルキー
  private[ this ] var tAutoPlacementKey = Array.empty[ String ]


  // トグルキーのプライマリキーとコンビネーションキーを生成する
  private[ this ] def toggleKey( desc: String,
                                 keys: Array[ String ] )
  : Option[ ( KeyBinding, Array[ Int ] ) ] =
  {
    if ( keys.isEmpty ) None
    else {
      import org.lwjgl.input.Keyboard

      def keycode( s: String ): Int = {
        val uc = s.toUpperCase
        val name = if ( uc.startsWith( "KEY_" ) ) uc.substring( 4 ) else uc
        Keyboard.getKeyIndex( name )
      }

      val primaryKey = keycode( keys.head )
      if ( primaryKey == Keyboard.KEY_NONE ) {
        mBreak.log.warn( s"Illegal primary keycode: ${ keys.head }" )
        None
      }
      else {
        val combinationKeys = keys.drop( 1 ).map( keycode )
        val illegalIndex = combinationKeys.indexWhere( _ == Keyboard.KEY_NONE )
        if ( illegalIndex != -1 ) {
          mBreak.log.warn( s"Illegal combination keycode: ${ keys( illegalIndex + 1 ) }" )
          None
        }
        else {
          Option { (
            new KeyBinding( desc, primaryKey, "key.categories.misc" ),
            combinationKeys
          ) }
      } }
  } }


  // 採掘速度乗数のゲッタ
  private[ moj_mbreak ] def mining_speedmult = mSpeedmult

  // 連鎖破壊フラグのゲッタ
  private[ moj_mbreak ] def chain_destruction = cDestruction

  // たいまつの自動設置フラグのゲッタ
  private[ moj_mbreak ] def torch_auto_placement = tAutoPlacement


  // 採掘高速化トグルキーを取得
  private[ moj_mbreak ] lazy val toggle_mining_speedmult =
    toggleKey( "moj_mbreak.key.toggle_mining_speedmult", mSpeedmultKey )

  // 連鎖破壊トグルキーを取得
  private[ moj_mbreak ] lazy val toggle_chain_destruction =
    toggleKey( "moj_mbreak.key.toggle_chain_destruction", cDestructionKey )

  // たいまつ自動設置トグルキーを取得
  private[ moj_mbreak ] lazy val toggle_torch_auto_placement =
    toggleKey( "moj_mbreak.key.toggle_torch_auto_placement", tAutoPlacementKey )

  /**
    * コンフィグからローカル変数へ値を読み出す
    * @param load コンフィグファイルから値を読み込むか否か
    */
  private[ this ] def syncConfig( load: Boolean ): Unit = config match {
    case Some( cfg ) =>
      def stra2str( arr: Array[ String ] ): String = arr.mkString( "[", ", ", "]" )

      if ( load && !cfg.isChild ) cfg.load()

      val order = Lists.newArrayList[ String ]
      var prop  = null: Property

      prop = cfg.get( CONFIG_ID, "mining_speedmult", 2.5 )
      prop.setMaxValue( 100 )
      prop.setMinValue(   0 )
      prop.setComment( i18n( "moj_mbreak.cfg.mining_speedmult.tooltip" ) )
      prop.setLanguageKey( "moj_mbreak.cfg.mining_speedmult" )
      order add prop.getName
      mSpeedmult = prop.getDouble.toFloat

      mBreak.log.debug( s"Bind mining_speedmult: $mSpeedmult" )

      prop = cfg.get( CONFIG_ID, "chain_destruction", true )
      prop.setComment( i18n( "moj_mbreak.cfg.chain_destruction.tooltip" ) )
      prop.setLanguageKey( "moj_mbreak.cfg.chain_destruction" )
      order add prop.getName
      cDestruction = prop.getBoolean

      mBreak.log.debug( s"Bind chain_destruction: $cDestruction" )

      prop = cfg.get( CONFIG_ID, "torch_auto_placement", true )
      prop.setComment( i18n( "moj_mbreak.cfg.torch_auto_placement.tooltip" ) )
      prop.setLanguageKey( "moj_mbreak.cfg.torch_auto_placement" )
      order add prop.getName
      tAutoPlacement = prop.getBoolean

      mBreak.log.debug( s"Bind torch_auto_placement: $tAutoPlacement" )

      prop = cfg.get( CONFIG_ID, "toggle_mining_speedmult", Array.empty[ String ] )
      prop.setComment( i18n( "moj_mbreak.cfg.toggle_mining_speedmult.tooltip" ) )
      prop.setLanguageKey( "moj_mbreak.cfg.toggle_mining_speedmult" )
      order add prop.getName
      mSpeedmultKey = prop.getStringList

      mBreak.log.debug( s"Bind toggle_mining_speedmult: ${ stra2str( mSpeedmultKey ) }" )

      prop = cfg.get( CONFIG_ID, "toggle_chain_destruction", Array.empty[ String ] )
      prop.setComment( i18n( "moj_mbreak.cfg.toggle_chain_destruction.tooltip" ) )
      prop.setLanguageKey( "moj_mbreak.cfg.toggle_chain_destruction" )
      order add prop.getName
      cDestructionKey = prop.getStringList

      mBreak.log.debug( s"Bind toggle_chain_destruction: ${ stra2str( cDestructionKey ) }" )

      prop = cfg.get( CONFIG_ID, "toggle_torch_auto_placement", Array.empty[ String ] )
      prop.setComment( i18n( "moj_mbreak.cfg.toggle_torch_auto_placement.tooltip" ) )
      prop.setLanguageKey( "moj_mbreak.cfg.toggle_torch_auto_placement" )
      order add prop.getName
      tAutoPlacementKey = prop.getStringList

      mBreak.log.debug( s"Bind toggle_torch_auto_placement: ${ stra2str( tAutoPlacementKey ) }" )

      cfg.setCategoryPropertyOrder( CONFIG_ID, order )

      if ( cfg.hasChanged ) cfg.save()
    case None        =>
      mBreak.log.warn( "Config is not bind to mBreakConfigHandler." )
  }


  /**
    * 指定されたコンフィグを読み込み、このハンドラにバインドする
    * @param cfg この Mod のコンフィグ
    */
  private[ moj_mbreak ] def captureConfig( cfg: Configuration ): Unit = {
    config = Option( cfg )
    syncConfig( true )
  }

  /**
    * この Mod のコンフィグの要素を返す
    * @return Config-Gui で取り扱うすべてのコンフィグの要素
    */
  private[ moj_mbreak ] def configElements = config match {
    case Some( cfg ) =>
      val cat = cfg.getCategory( CONFIG_ID )
      new ConfigElement( cat ).getChildElements
    case None        =>
      mBreak.log.warn( "Config is not bind to mBreakConfigHandler." )
      Collections.emptyList[ IConfigElement ]
  }


  /**
    * Config-Gui 経由でコンフィグが変更されたときのイベントをフック
    * 変更されたコンフィグの値をハンドラの変数に反映する
    * @param evt コンフィグ変更イベント
    */
  @SubscribeEvent
  def onConfigChanged( evt: OnConfigChangedEvent ): Unit = {
    import evt._
    if ( getModID == MOD_ID && getConfigID == CONFIG_ID ) {
      syncConfig( false )
    }
  }
}
