package io.github.karino2.rhinocs

class BufferCollection {
    val bufferMap = mutableMapOf<String, Buffer>()

    fun clear() { bufferMap.clear() }

    fun newName(nameCand: String) : String {
        if(!bufferMap.containsKey(nameCand))
            return nameCand

        var i = 1
        while(true) {
            val name = "${nameCand}-${i}"
            if(!bufferMap.containsKey(name))
                return name
            i += 1
        }
    }

    // 基本的居にはnameCandのBufferを作るけれど、
    // 既にnameCandのバッファがあったらその後ろに-1, -2, ...とつける
    fun newBuffer(nameCand: String) : Buffer {
        val bname = newName(nameCand)
        val buf = Buffer(bname)
        bufferMap[bname] = buf
        return buf
    }

    // nameのバッファがあれば返す、なければ新しくnameのバッファを作って返す
    fun getBufferCreate(name: String): Buffer {
        bufferMap[name]?.let { return it }

        val buffer = Buffer(name)
        bufferMap[name] = buffer
        return buffer
    }
}