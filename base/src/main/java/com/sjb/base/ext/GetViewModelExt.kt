package com.sjb.base.ext

import java.lang.reflect.ParameterizedType


/**
 *    author : MJ
 *    time   : 2022/05/23
 *    desc   : 获取viewModel
 */

/**
 *  获取当前类绑定的泛型ViewModel-clazz
 */
fun <VM> getVmClazz(obj:Any):VM{
    return(obj.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as VM
}


