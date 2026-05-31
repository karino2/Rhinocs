package io.github.karino2.rhinocs

import android.net.Uri

class BufferCollection {
    val nameMap = mutableMapOf<String, Buffer>()
    // uriは存在しない場合はこのマップには入らない。
    val uriMap = mutableMapOf<Uri, Buffer>()

    fun clear() {
        nameMap.clear()
        uriMap.clear()
    }

    val buffers: List<Buffer>
        get() = nameMap.values.toList()

    fun newName(nameCand: String) : String {
        if(!nameMap.containsKey(nameCand))
            return nameCand

        var i = 1
        while(true) {
            val name = "${nameCand}-${i}"
            if(!nameMap.containsKey(name))
                return name
            i += 1
        }
    }

    // 基本的居にはnameCandのBufferを作るけれど、
    // 既にnameCandのバッファがあったらその後ろに-1, -2, ...とつける
    fun newBuffer(nameCand: String, uri: Uri? = null) : Buffer {
        val bname = newName(nameCand)
        val buf = Buffer(bname)
        nameMap[bname] = buf
        uri?.let {
            uriMap[it] = buf
        }
        return buf
    }

    /*
      既にbufferMapにあるはずのbufの名前を変更する。
     */
    fun renameBuffer(buf: Buffer, nameCand: String, uri: Uri) {
        nameMap.remove(buf.name)
        val bname = newName(nameCand)
        buf.name = bname
        nameMap[bname] = buf
        uriMap[uri] = buf
    }


    // nameのバッファがあれば返す、なければ新しくnameのバッファを作って返す
    fun getBufferCreate(name: String): Buffer {
        nameMap[name]?.let { return it }

        val buffer = Buffer(name)
        nameMap[name] = buffer
        return buffer
    }

    fun getByUri(uri: Uri) : Buffer? {
        return uriMap[uri]
    }
}