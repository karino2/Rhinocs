package io.github.karino2.rhinocs

/*
    キーボードなどのデバイスコンフィグレーションの変化でActivityがリサイクルされる時に、
    それをまたいで保持して欲しいものを置く。
    ただしプロセスがkillされるケースでは無くなる事はあるので、getは無ければnewと振る舞う。

    リセットなどは明示的に行う。
 */
object Cache {
    fun clear() {
        _interpreter = null
        _rhinocs = null
    }

    private var _interpreter: Interpreter? = null

    val isInterPreterExists: Boolean
        get() = _interpreter != null

    fun getInterpreter(activity: MainActivity, rview: RView) : Interpreter {
        if (_interpreter == null) {
            _interpreter = activity.newInterpreter()
        }
        _interpreter?.global?.reassign(activity, rview)
        return _interpreter!!
    }


    private var _rhinocs: Rhinocs? = null
    val rhinocs : Rhinocs
        get() {
            if (_rhinocs != null) {
                return _rhinocs!!
            }
            _rhinocs = Rhinocs()
            return _rhinocs!!
        }

    val isRhinocsExists: Boolean
        get() = _rhinocs != null

}