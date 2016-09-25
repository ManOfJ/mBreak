package com.manofj.minecraft.moj_mbreak

import net.minecraft.launchwrapper.Launch

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent

import com.manofj.commons.minecraftforge.base.MinecraftForgeMod
import com.manofj.commons.minecraftforge.i18n.I18nSupportMod
import com.manofj.commons.minecraftforge.logging.LoggerLikeMod


@Mod( modid       = mBreak.modId,
      name        = mBreak.modName,
      version     = mBreak.modVersion,
      modLanguage = mBreak.modLanguage,
      updateJSON  = mBreak.updateJSON,
      guiFactory  = mBreak.guiFactory )
object mBreak
  extends MinecraftForgeMod
  with    I18nSupportMod
  with    LoggerLikeMod
{
  final val modId      = "moj_mbreak"
  final val modName    = "mBreak"
  final val modVersion = "@version@"
  final val updateJSON = "@updateJson@"
  final val guiFactory = "com.manofj.minecraft.moj_mbreak.mBreakGuiFactory"


  {
    Launch.classLoader.addTransformerExclusion( "akka." )
  }


  @EventHandler
  def preInit( evt: FMLPreInitializationEvent ): Unit = {
    mBreakConfig.capture( evt.getSuggestedConfigurationFile )

    MinecraftForge.EVENT_BUS.register( mBreakConfig )
    MinecraftForge.EVENT_BUS.register( mBreakEventHandler )

    ClientRegistry.registerKeyBinding( mBreakConfig.miningSpeedMultToggleKey )
    ClientRegistry.registerKeyBinding( mBreakConfig.chainDestructionToggleKey )
    ClientRegistry.registerKeyBinding( mBreakConfig.torchAutoPlacementToggleKey )
    ClientRegistry.registerKeyBinding( mBreakConfig.torchPlacementSideToggleKey )

  }

}
