package com.manofj.minecraft.moj_mbreak

import java.util.Locale
import java.util.regex.Pattern

import com.google.common.collect.Lists
import org.lwjgl.input.Keyboard

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.settings.KeyBinding

import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.client.settings.KeyModifier
import net.minecraftforge.common.config.Property

import com.manofj.commons.scala.util.conversions.Any$

import com.manofj.commons.minecraftforge.config.ForgeConfig
import com.manofj.commons.minecraftforge.config.SimpleConfig
import com.manofj.commons.minecraftforge.config.gui.ConfigGui
import com.manofj.commons.minecraftforge.config.gui.SimpleGuiFactory
import com.manofj.commons.minecraftforge.config.gui.SimpleGuiParams


object mBreakConfig
  extends SimpleConfig
{
  private[ this ] final val MINING_SPEED_MULT        = "mining_speedmult"
  private[ this ] final val MINING_SPEED_MULT_ENABLE = "mining_speedmult_enable"
  private[ this ] final val CHAIN_DESTRUCTION        = "chain_destruction"
  private[ this ] final val TORCH_AUTO_PLACEMENT     = "torch_auto_placement"
  private[ this ] final val TORCH_PLACEMENT_SIDE     = "torch_placement_side"


  override protected final val modId: String         = mBreak.modId
  override protected final val configVersion: String = "1.0.0"


  private[ this ] var miningSpeedmultOpt = Option.empty[ Double ]
  def miningSpeedmult: Double = miningSpeedmultOpt.getOrElse( 2.5d )
  def miningSpeedmult_=( v: Double ): Unit = {
    miningSpeedmultOpt = v.?
    theConfig.getCategory( configId ).get( MINING_SPEED_MULT ).?.foreach( _.set( miningSpeedmult ) )
    save()
  }

  private[ this ] var miningSpeedmultEnableOpt = Option.empty[ Boolean ]
  def miningSpeedmultEnable: Boolean = miningSpeedmultEnableOpt.getOrElse( true )
  def miningSpeedmultEnable_=( v: Boolean ): Unit = {
    miningSpeedmultEnableOpt = v.?
    theConfig.getCategory( configId ).get( MINING_SPEED_MULT_ENABLE ).?.foreach( _.set( miningSpeedmultEnable ) )
    save()
  }

  private[ this ] var chainDestructionOpt = Option.empty[ Boolean ]
  def chainDestruction: Boolean = chainDestructionOpt.getOrElse( true )
  def chainDestruction_=( v: Boolean ): Unit = {
    chainDestructionOpt = v.?
    theConfig.getCategory( configId ).get( CHAIN_DESTRUCTION ).?.foreach( _.set( chainDestruction ) )
    save()
  }

  private[ this ] var torchAutoPlacementOpt = Option.empty[ Boolean ]
  def torchAutoPlacement: Boolean = torchAutoPlacementOpt.getOrElse( true )
  def torchAutoPlacement_=( v: Boolean ): Unit = {
    torchAutoPlacementOpt = v.?
    theConfig.getCategory( configId ).get( TORCH_AUTO_PLACEMENT ).?.foreach( _.set( torchAutoPlacement ) )
    save()
  }

  private[ this ] val torchPlacementSideValidator = Pattern.compile( "(?i)^(right|left)$" )
  private[ this ] var torchPlacementSideOpt = Option.empty[ String ]
  def torchPlacementSide: String = torchPlacementSideOpt.filter( _ == "right" ).getOrElse( "left" )
  def torchPlacementSide_=( v: String ): Unit = {
    torchPlacementSideOpt = v.?.map( _.toLowerCase( Locale.ENGLISH ) )
    theConfig.getCategory( configId ).get( TORCH_PLACEMENT_SIDE ).?.foreach( _.set( torchPlacementSide ) )
    save()
  }

  final val miningSpeedMultToggleKey = new KeyBinding(
    mBreak.languageKey( "key.mining_speedmult" ),
    KeyConflictContext.IN_GAME,
    KeyModifier.NONE,
    Keyboard.KEY_NONE,
    "key.categories.misc"
  )

  final val chainDestructionToggleKey = new KeyBinding(
    mBreak.languageKey( "key.chain_destruction" ),
    KeyConflictContext.IN_GAME,
    KeyModifier.NONE,
    Keyboard.KEY_NONE,
    "key.categories.misc"
  )

  final val torchAutoPlacementToggleKey = new KeyBinding(
    mBreak.languageKey( "key.torch_auto_placement" ),
    KeyConflictContext.IN_GAME,
    KeyModifier.NONE,
    Keyboard.KEY_NONE,
    "key.categories.misc"
  )

  final val torchPlacementSideToggleKey = new KeyBinding(
    mBreak.languageKey( "key.torch_placement_side" ),
    KeyConflictContext.IN_GAME,
    KeyModifier.NONE,
    Keyboard.KEY_NONE,
    "key.categories.misc"
  )


  // 廃止されたコンフィグ項目の削除など
  override def fix( definedVersion: String, loadedVersion: String ): Unit = {
    val category = theConfig.getCategory( configId )

    loadedVersion match {
      case x if x == definedVersion => // 何もしない
      case "" =>
        category.remove( "toggle_mining_speedmult" )
        category.remove( "toggle_chain_destruction" )
        category.remove( "toggle_torch_auto_placement" )

      case any => mBreak.warn( s"Loaded version is unknown: $any" )
    }
  }

  override def sync(): Unit = {
    def languageKey( k: String, s: String = "" ) = mBreak.languageKey( k, "cfg", s )
    def comment( k: String ) = mBreak.message( languageKey( k, "tooltip" ) )

    val cfg      = theConfig
    val order    = Lists.newArrayList[ String ]
    var prop     = null: Property

    prop = cfg.get( configId, MINING_SPEED_MULT, 2.5D )
    prop.setMaxValue( 100D )
    prop.setMinValue(   1D )
    prop.setComment( comment( MINING_SPEED_MULT ) )
    prop.setLanguageKey( languageKey( MINING_SPEED_MULT ) )
    order add prop.getName
    miningSpeedmultOpt = prop.getDouble.?

    prop = cfg.get( configId, MINING_SPEED_MULT_ENABLE, true )
    prop.setComment( comment( MINING_SPEED_MULT_ENABLE ) )
    prop.setLanguageKey( languageKey( MINING_SPEED_MULT_ENABLE ) )
    order add prop.getName
    miningSpeedmultEnableOpt = prop.getBoolean.?

    prop = cfg.get( configId, CHAIN_DESTRUCTION, true )
    prop.setComment( comment( CHAIN_DESTRUCTION ) )
    prop.setLanguageKey( languageKey( CHAIN_DESTRUCTION ) )
    order add prop.getName
    chainDestructionOpt = prop.getBoolean.?

    prop = cfg.get( configId, TORCH_AUTO_PLACEMENT, true )
    prop.setComment( comment( TORCH_AUTO_PLACEMENT ) )
    prop.setLanguageKey( languageKey( TORCH_AUTO_PLACEMENT ) )
    order add prop.getName
    torchAutoPlacementOpt = prop.getBoolean.?

    prop = cfg.get( configId, TORCH_PLACEMENT_SIDE, "left" )
    prop.setComment( comment( TORCH_PLACEMENT_SIDE ) )
    prop.setLanguageKey( languageKey( TORCH_PLACEMENT_SIDE ) )
    prop.setValidationPattern( torchPlacementSideValidator )
    order add prop.getName
    torchPlacementSideOpt = prop.getString.?.map( _.toLowerCase( Locale.ENGLISH ) )

    cfg.setCategoryPropertyOrder( configId, order )

  }

}

object mBreakGuiParams
  extends SimpleGuiParams
{
  override protected final val config: ForgeConfig = mBreakConfig.theConfig
  override final val modId: String = mBreak.modId
  override def title: String = mBreak.format( "cfg.gui.title" )
}

class mBreakConfigGui( parent: GuiScreen ) extends ConfigGui( parent, mBreakGuiParams )
class mBreakGuiFactory extends SimpleGuiFactory[ mBreakConfigGui ]
