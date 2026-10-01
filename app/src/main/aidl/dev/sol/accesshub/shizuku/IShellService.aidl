// Runs inside the Shizuku (shell-uid) process. Shizuku instantiates ShellUserService via
// bindUserService(); the app calls exec() over Binder to run privileged `settings` commands.
package dev.sol.accesshub.shizuku;

interface IShellService {
    // Executes `/system/bin/sh -c <script>` and returns a NUL-separated
    // "<exitCode>\0<stdout>\0<stderr>" string.
    String exec(String script) = 1;

    // Shizuku's well-known cleanup transaction (Shizuku-API README: transaction 16777115,
    // declared as 16777114 here). It is called on the old instance when the service version
    // changes, e.g. after an app update.
    void destroy() = 16777114;
}
