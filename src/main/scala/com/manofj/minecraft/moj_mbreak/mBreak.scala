package com.manofj.minecraft.moj_mbreak

import net.minecraft.launchwrapper.Launch

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent

import com.manofj.minecraft.moj_commons.logging.LoggerLikeMod
import com.manofj.minecraft.moj_commons.util.MinecraftMod


@Mod( modid       = mBreak.modId,
      name        = mBreak.modName,
      version     = mBreak.modVersion,
      guiFactory  = mBreak.guiFactory,
      modLanguage = "scala" )
object mBreak
  extends MinecraftMod
  with    LoggerLikeMod
{

  final val modId      = "@modid@"
  final val modName    = "mBreak"
  final val modVersion = "@version@"
  final val guiFactory = "com.manofj.minecraft.moj_mbreak.mBreakGuiFactory"


  // 初期化ブロック
  {
    // akka のパッケージをトランスフォーム対象外に
    Launch.classLoader.addTransformerExclusion( "akka." )
  }


  @EventHandler
  def preInit( evt: FMLPreInitializationEvent ): Unit = {
    import com.manofj.minecraft.moj_commons.config.javaFile2ForgeConfig

    mBreakConfigHandler.captureConfig( evt.getSuggestedConfigurationFile )

    MinecraftForge.EVENT_BUS.register( mBreakEventHandler )
    MinecraftForge.EVENT_BUS.register( mBreakConfigHandler )

    ClientRegistry.registerKeyBinding( mBreakSettings.miningSpeedMultToggleKey )
    ClientRegistry.registerKeyBinding( mBreakSettings.chainDestructionToggleKey )
    ClientRegistry.registerKeyBinding( mBreakSettings.torchAutoPlacementToggleKey )

  }

}
