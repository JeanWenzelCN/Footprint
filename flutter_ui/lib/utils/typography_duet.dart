import 'package:flutter/material.dart';

/// 【Typography Duet】字体栈的双重奏
/// 
/// 该工具类将极具张力的中文字体与优雅的英文衬线体/手写体完美融合。
class TypographyDuet {
  
  /// 英文手写体 (如 Dancing Script) + 小号黑体中文字库叠加在上方
  /// 提供一种原版英文杂志(Editorial Design) 质感的排版配置
  static TextStyle get englishHandwriting => const TextStyle(
        fontFamily: 'DancingScript',
        fontSize: 22.0,
        height: 1.4,
        color: Color(0xFF8A8A8A), // 浅灰色作为背景底纹
        letterSpacing: 1.2,
      );

  static TextStyle get englishSerif => const TextStyle(
        fontFamily: 'PlayfairDisplay',
        fontSize: 18.0,
        height: 1.5,
        color: Color(0xFF2C2C2C),
        letterSpacing: 0.5,
      );

  static TextStyle get chineseTranslation => const TextStyle(
        fontFamily: 'MaShanZheng', // 或无衬线黑体叠加
        fontSize: 12.0,
        fontWeight: FontWeight.w400,
        color: Color(0xFF4A4A4A),
        letterSpacing: 1.5,
        height: 1.6,
      );

  /// “译者模式” - AI 隐藏指令
  static String appendTranslatorModePrompt(String originalPrompt) {
    return '$originalPrompt\n\n'
           '【隐藏指令 / Translator Mode】\n'
           '在这个回忆日记的末尾，请自动匹配一句极其精准、隽永的英文诗句或经典电影台词。'
           '这句英文必须与刚才记录的情感和氛围完美契合，它代表着岁月沉淀和异地迁延中的思念。'
           '直接输出英文句子及其中文翻译，不要加任何其他解释。';
  }
  
  /// A widget to render the Editorial Design magazine-like bottom text
  /// 一个精致的弹框底部渲染视图，用于呈现 Editorial Design 杂志质感的叠加文字
  static Widget buildTranslatorBottomView(String englishText, String chineseText) {
    return Container(
      constraints: const BoxConstraints(minHeight: 80),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          // 底层：轻盈、张扬的英文手写体底纹
          Opacity(
            opacity: 0.15,
            child: Text(
              englishText,
              style: englishHandwriting.copyWith(
                fontSize: 32,
                color: const Color(0xFF2C3E50),
              ),
              softWrap: true,
              maxLines: 2,
              overflow: TextOverflow.visible,
            ),
          ),
          // 顶层：克制、精准的中文意译
          Padding(
            padding: const EdgeInsets.only(left: 4, top: 12),
            child: Text(
              chineseText,
              style: chineseTranslation.copyWith(
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: const Color(0xFF34495E),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
