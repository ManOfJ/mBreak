package com.manofj.minecraft.moj_mbreak

import com.google.common.collect.Lists

import net.minecraft.client.resources.I18n.{ format => i18n }
import net.minecraft.client.settings.KeyBinding

import net.minecraftforge.common.config.Property

import com.manofj.minecraft.moj_commons.config.ConfigGuiHandler


/**
  * Mod のコンフィグハンドラ
  * コンフィグの読み込みや、変数へのバインドなどを行う
  */
object mBreakConfigHandler extends ConfigGuiHandler {

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


  override val modId: String = MOD_ID

  override val title: String = i18n( "moj_mbreak.config.gui.title" )

  override def syncConfig( load: Boolean ): Unit = {
    def stra2str( arr: Array[ String ] ): String = arr.mkString( "[", ", ", "]" )


    val cfg = config

    if ( load && !cfg.isChild ) cfg.load()

    val order = Lists.newArrayList[ String ]
    var prop  = null: Property

    prop = cfg.get( configId, "mining_speedmult", 2.5 )
    prop.setMaxValue( 100 )
    prop.setMinValue(   0 )
    prop.setComment( i18n( "moj_mbreak.cfg.mining_speedmult.tooltip" ) )
    prop.setLanguageKey( "moj_mbreak.cfg.mining_speedmult" )
    order add prop.getName
    mSpeedmult = prop.getDouble.toFloat

    mBreak.log.debug( s"Bind mining_speedmult: $mSpeedmult" )

    prop = cfg.get( configId, "chain_destruction", true )
    prop.setComment( i18n( "moj_mbreak.cfg.chain_destruction.tooltip" ) )
    prop.setLanguageKey( "moj_mbreak.cfg.chain_destruction" )
    order add prop.getName
    cDestruction = prop.getBoolean

    mBreak.log.debug( s"Bind chain_destruction: $cDestruction" )

    prop = cfg.get( configId, "torch_auto_placement", true )
    prop.setComment( i18n( "moj_mbreak.cfg.torch_auto_placement.tooltip" ) )
    prop.setLanguageKey( "moj_mbreak.cfg.torch_auto_placement" )
    order add prop.getName
    tAutoPlacement = prop.getBoolean

    mBreak.log.debug( s"Bind torch_auto_placement: $tAutoPlacement" )

    prop = cfg.get( configId, "toggle_mining_speedmult", Array.empty[ String ] )
    prop.setComment( i18n( "moj_mbreak.cfg.toggle_mining_speedmult.tooltip" ) )
    prop.setLanguageKey( "moj_mbreak.cfg.toggle_mining_speedmult" )
    order add prop.getName
    mSpeedmultKey = prop.getStringList

    mBreak.log.debug( s"Bind toggle_mining_speedmult: ${ stra2str( mSpeedmultKey ) }" )

    prop = cfg.get( configId, "toggle_chain_destruction", Array.empty[ String ] )
    prop.setComment( i18n( "moj_mbreak.cfg.toggle_chain_destruction.tooltip" ) )
    prop.setLanguageKey( "moj_mbreak.cfg.toggle_chain_destruction" )
    order add prop.getName
    cDestructionKey = prop.getStringList

    mBreak.log.debug( s"Bind toggle_chain_destruction: ${ stra2str( cDestructionKey ) }" )

    prop = cfg.get( configId, "toggle_torch_auto_placement", Array.empty[ String ] )
    prop.setComment( i18n( "moj_mbreak.cfg.toggle_torch_auto_placement.tooltip" ) )
    prop.setLanguageKey( "moj_mbreak.cfg.toggle_torch_auto_placement" )
    order add prop.getName
    tAutoPlacementKey = prop.getStringList

    mBreak.log.debug( s"Bind toggle_torch_auto_placement: ${ stra2str( tAutoPlacementKey ) }" )

    cfg.setCategoryPropertyOrder( configId, order )

    if ( cfg.hasChanged ) cfg.save()
  }
}
