package com.manofj.minecraft.moj_mbreak


import net.minecraft.client.gui.GuiScreen

import com.manofj.minecraft.moj_commons.config.{ ConfigGui, ConfigGuiFactory }


class mBreakConfigGui( parent: GuiScreen ) extends ConfigGui( parent, mBreakConfigHandler )
class mBreakGuiFactory extends ConfigGuiFactory[ mBreakConfigGui ]
