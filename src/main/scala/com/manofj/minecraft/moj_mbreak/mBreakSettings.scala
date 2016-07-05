package com.manofj.minecraft.moj_mbreak

import scala.reflect.ClassTag

import org.lwjgl.input.Keyboard

import net.minecraft.client.settings.KeyBinding

import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.client.settings.KeyModifier

import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension


private[ moj_mbreak ] object mBreakSettings {

  final val MINING_SPEED_MULT_KEY = "mining_speedmult"
  final val MINING_SPEED_MULT_DEFAULT = 2.5d

  final val CHAIN_DESTRUCTION_KEY = "chain_destruction"
  final val CHAIN_DESTRUCTION_DEFAULT = true

  final val TORCH_AUTO_PLACEMENT_KEY = "torch_auto_placement"
  final val TORCH_AUTO_PLACEMENT_DEFAULT = true


  private[ this ] var miningSpeedMultOpt = Option.empty[ Double ]
  def miningSpeedMult: Double = miningSpeedMultOpt.getOrElse( MINING_SPEED_MULT_DEFAULT )

  private[ this ] var chainDestructionOpt = Option.empty[ Boolean ]
  def chainDestruction: Boolean = chainDestructionOpt.getOrElse( CHAIN_DESTRUCTION_DEFAULT )

  private[ this ] var torchAutoPlacementOpt = Option.empty[ Boolean ]
  def torchAutoPlacement: Boolean = torchAutoPlacementOpt.getOrElse( TORCH_AUTO_PLACEMENT_DEFAULT )


  lazy val miningSpeedMultToggleKey = new KeyBinding(
    "moj_mbreak.key.mining_speedmult",
    KeyConflictContext.IN_GAME,
    KeyModifier.NONE,
    Keyboard.KEY_NONE,
    "key.categories.misc"
  )

  lazy val chainDestructionToggleKey = new KeyBinding(
    "moj_mbreak.key.chain_destruction",
    KeyConflictContext.IN_GAME,
    KeyModifier.NONE,
    Keyboard.KEY_NONE,
    "key.categories.misc"
  )

  lazy val torchAutoPlacementToggleKey = new KeyBinding(
    "moj_mbreak.key.torch_auto_placement",
    KeyConflictContext.IN_GAME,
    KeyModifier.NONE,
    Keyboard.KEY_NONE,
    "key.categories.misc"
  )


  def reflectConfigChanges( data: Map[ String, Any ] ): Unit = {
    def value[ A : ClassTag ]( key: String ): A = data( key ).asInstanceOf[ A ]

    miningSpeedMultOpt    = value[ Double ]( MINING_SPEED_MULT_KEY ).?
    chainDestructionOpt   = value[ Boolean ]( CHAIN_DESTRUCTION_KEY ).?
    torchAutoPlacementOpt = value[ Boolean ]( TORCH_AUTO_PLACEMENT_KEY ).?

  }


  object Local {

    var miningSpeedMult = mBreakSettings.miningSpeedMult > 1d
    var chainDestruction = mBreakSettings.chainDestruction
    var torchAutoPlacement = mBreakSettings.torchAutoPlacement

  }

}
