package io.github.karino2.rhinocs

/*
import ru.tetraquark.kmplibs.myersdiffkt.Change
import ru.tetraquark.kmplibs.myersdiffkt.DiffUtil

 */

sealed class DiffOp {
    data class Insert(val toOldIndex: Int, val fromNewIndex: Int, val count: Int) : DiffOp()
    data class Remove(val index: Int, val count: Int) : DiffOp()
}

class Diff(val oldList: List<String>, val newList: List<String>) {
    fun calculate(): List<DiffOp> {
        val diffResult = DiffUtil.calculateDiff(
            object : DiffUtil.Callback {
                override fun getOldListSize(): Int {
                    return oldList.size
                }

                override fun getNewListSize(): Int {
                    return newList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition] == newList[newItemPosition]
                }
            }
        )

        val ops = mutableListOf<DiffOp>()
        diffResult.dispatchUpdatesTo(object : DiffUtil.DiffResult.ListUpdateCallback {
            override fun onRemove(index: Int, count: Int) {
                ops.add(DiffOp.Remove(index, count))
        }

            override fun onInsert(
                oldIndex: Int,
                fromNewIndex: Int,
                count: Int
            ) {
                ops.add(DiffOp.Insert(oldIndex, fromNewIndex, count))
            }

        })
        return ops
    }
}

/*
Porting DiffUtil from RecyclerView:
[recyclerview/recyclerview/src/main/java/androidx/recyclerview/widget/DiffUtil.java - platform/frameworks/support - Git at Google](https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/recyclerview/recyclerview/src/main/java/androidx/recyclerview/widget/DiffUtil.java#236)
*/

/*
class Diff(val oldList: List<String>, val newList: List<String>) {
    fun calculate(): List<DiffOp> {
        val diffResult = DiffUtil.diffCallback(
            oldListSize = oldList.size,
            newListSize = newList.size,
            comparator = { oldIdx, newIdx -> oldList[oldIdx] == newList[newIdx] }
        )

        val ops = mutableListOf<DiffOp>()
        diffResult.applyChanges { change ->
            when (change) {
                is Change.Insert -> ops.add(DiffOp.Insert(change.toOldListIndex, change.fromNewListIndex, change.count))
                is Change.Remove -> ops.add(DiffOp.Remove(change.fromOldListIndex, change.count))
            }
        }
        return ops
    }
}
*/