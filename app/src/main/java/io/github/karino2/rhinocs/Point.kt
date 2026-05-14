package io.github.karino2.rhinocs

/*
  何行目かとその行から何文字めか。offsetは文字であってカラムでは無いので注意。
  linenumはrowと全く同じものだが、PointはどちらかといえばBuffer内の文字の位置を表すもので、
  行数とoffsetなのは現在のバッファが行ごとの配列になっているからという事情による。
 */
data class Point(val linenum: Int, val offset: Int)