package dev.sol.accesshub.shizuku

import android.os.Process
import android.util.Log

class ShellUserService {
    fun doSomething() {
        Log.d("ShellUserService", "Running in Shizuku process, uid=" + Process.myUid())
    }
}
