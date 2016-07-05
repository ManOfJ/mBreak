package com.manofj.minecraft.moj_mbreak

import com.google.common.collect.Lists

import net.minecraft.client.gui.GuiScreen

import net.minecraftforge.common.config.Property

import com.manofj.minecraft.moj_commons.config.ConfigGui
import com.manofj.minecraft.moj_commons.config.ConfigGuiFactory
import com.manofj.minecraft.moj_commons.config.ConfigGuiHandler
import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension


object mBreakConfigHandler
  extends ConfigGuiHandler
{
  import net.minecraft.client.resources.I18n.{ format => l10n }

  import com.manofj.minecraft.moj_mbreak.mBreakSettings._


  private[ this ] def languageKey( key: String ): String = s"$modId.cfg.$key"
  private[ this ] def comment( key: String ): String = l10n( languageKey( key ) + ".tooltip" )


  override val modId: String = mBreak.modId

  override val title: String = l10n( "moj_mbreak.config.gui.title" )


  override def syncConfig( load: Boolean ): Unit = {
    val cfg = config

    if ( load && !cfg.isChild ) cfg.load()

    // 廃止となったコンフィグ項目の削除
    cfg.getCategory( configId ) << { category =>
      category.remove( "toggle_mining_speedmult" )
      category.remove( "toggle_chain_destruction" )
      category.remove( "toggle_torch_auto_placement" )
    }

    val order = Lists.newArrayList[ String ]
    val settings = Map.newBuilder[ String, Any ]
    var prop  = null: Property

    prop = cfg.get( configId, MINING_SPEED_MULT_KEY, MINING_SPEED_MULT_DEFAULT )
    prop.setMaxValue( 100 )
    prop.setMinValue(   0 )
    prop.setComment( comment( MINING_SPEED_MULT_KEY ) )
    prop.setLanguageKey( languageKey( MINING_SPEED_MULT_KEY ) )
    order add prop.getName
    settings += MINING_SPEED_MULT_KEY -> prop.getDouble

    prop = cfg.get( configId, CHAIN_DESTRUCTION_KEY, CHAIN_DESTRUCTION_DEFAULT )
    prop.setComment( comment( CHAIN_DESTRUCTION_KEY ) )
    prop.setLanguageKey( languageKey( CHAIN_DESTRUCTION_KEY ) )
    order add prop.getName
    settings += CHAIN_DESTRUCTION_KEY -> prop.getBoolean

    prop = cfg.get( configId, TORCH_AUTO_PLACEMENT_KEY, TORCH_AUTO_PLACEMENT_DEFAULT )
    prop.setComment( comment( TORCH_AUTO_PLACEMENT_KEY ) )
    prop.setLanguageKey( languageKey( TORCH_AUTO_PLACEMENT_KEY ) )
    order add prop.getName
    settings += TORCH_AUTO_PLACEMENT_KEY -> prop.getBoolean

    cfg.setCategoryPropertyOrder( configId, order )

    if ( cfg.hasChanged ) cfg.save()

    mBreakSettings.reflectConfigChanges( settings.result )
  }
}

class mBreakConfigGui( parent: GuiScreen ) extends ConfigGui( parent, mBreakConfigHandler )
class mBreakGuiFactory extends ConfigGuiFactory[ mBreakConfigGui ]
