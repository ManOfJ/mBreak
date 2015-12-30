package manofj.com.github.moj_mbreak

import java.util.{ Set => JSet }

import manofj.com.github.moj_mbreak.mBreakConfigHandler.{ CONFIG_ID, configElements }
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.client.IModGuiFactory.{ RuntimeOptionCategoryElement, RuntimeOptionGuiHandler }
import net.minecraftforge.fml.client.config.GuiConfig


/**
  * Mod の GUI を生成するファクトリクラス
  */
class mBreakGuiFactory
  extends
    IModGuiFactory
{
  override def runtimeGuiCategories(): JSet[ RuntimeOptionCategoryElement ] = null

  override def initialize( minecraftInstance: Minecraft ): Unit = {}

  override def getHandlerFor( element: RuntimeOptionCategoryElement ): RuntimeOptionGuiHandler = null

  override def mainConfigGuiClass(): Class[ _ <: GuiScreen ] = classOf[ mBreakConfigGui ]
}


/**
  * Mod のコンフィグ GUI
  * @param guiScreen 親画面
  */
class mBreakConfigGui( guiScreen: GuiScreen )
  extends
    GuiConfig( guiScreen,
               configElements,
               MOD_ID,
               CONFIG_ID,
               false,
               false,
               I18n.format( "moj_mbreak.config.gui.title" ) )