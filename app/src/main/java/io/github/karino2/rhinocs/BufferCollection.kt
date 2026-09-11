package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcel
import androidx.core.net.toUri
import io.github.karino2.fastfile.FastFile

class BufferCollection {
    val nameMap = mutableMapOf<String, Buffer>()
    // uriは存在しない場合はこのマップには入らない。
    val uriMap = mutableMapOf<Uri, Buffer>()

    /*
        Parcelへのロードとストア
     */
    fun saveTo(out: Parcel) {
        out.writeInt(nameMap.size)
        nameMap.forEach {
            out.writeString(it.key)
            // nullも書いていいらしい
            out.writeString(it.value.url?.toString())
        }
    }

    fun loadFrom(from: Parcel, resolver: ContentResolver) {
        val bufNum = from.readInt()
        for(i in 0 until bufNum) {
            val name = from.readString()!!
            val surl = from.readString()

            val buf = Buffer(name)
            nameMap[name] = buf
            surl?.let {
                val uri = surl.toUri()
                uriMap[uri] = buf
                buf.url = uri
                FastFile.fromDocUri(resolver, uri)?.let {
                    buf.load(it.readText())
                }
            }
        }
    }

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
            buf.url = it
        }
        return buf
    }

    fun loadFromUri(uri: Uri, resolver: ContentResolver) : Buffer? {
        return  FastFile.fromDocUri(resolver, uri)?.let {
            val buf = newBuffer(it.name, uri)
            buf.load(it.readText())
            buf
        }
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

    // 必ずnameがある前提のget。ロードとストアなどの特殊な用途
    fun getByName(name: String) = nameMap[name]!!

    fun getByUri(uri: Uri) : Buffer? {
        return uriMap[uri]
    }
}