package com.manofj.minecraft

/**
  * mBreak-Mod のルートパッケージオブジェクト
  * 外部から参照可能な基本情報を定義しておく
  */
package object moj_mbreak {
  final val MOD_ID      = "@modid@"
  final val NAME        = "mBreak"
  final val VERSION     = "@version@"
  final val UPDATE_JSON = "http://manofj.com/minecraft/update?v=mBreak"
  final val LANGUAGE    = "scala"
  final val GUI_FACTORY = "com.manofj.minecraft.moj_mbreak.mBreakGuiFactory"
}
