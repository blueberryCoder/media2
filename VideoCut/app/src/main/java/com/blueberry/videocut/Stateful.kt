package com.blueberry.videocut

/**
 * Created by muyonggang on 2022/9/12
 * @author muyonggang@bytedance.com
 */
class Stateful<T> {

    private var code: Int = 0

    fun isSuccess(): Boolean {
        return code >= 0
    }


}