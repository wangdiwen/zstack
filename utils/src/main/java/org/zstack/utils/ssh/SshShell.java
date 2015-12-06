package org.zstack.utils.ssh;

import org.apache.commons.io.FileUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;

import java.io.File;

import static org.zstack.utils.StringDSL.ln;

/**
 * Created by frank on 12/5/2015.
 */
public class SshShell {
    private String hostname;
    private String username;
    private String password;
    private String privateKeyFile;
    private int port = 22;

    private void checkParams() {
        DebugUtils.Assert(hostname != null, "hostname cannot be null");
        DebugUtils.Assert(username != null, "username cannot be null");
        DebugUtils.Assert(password != null || privateKeyFile != null, "password and privateKeyFile must have at least one set");
    }

    public SshResult runCommand(String cmd) {
        checkParams();
        String ssh;
        File tempPasswordFile = null;

        try {
            if (privateKeyFile != null) {
                ssh = String.format("ssh -i %s -o UserKnownHostsFile=/dev/null -o PasswordAuthentication=no -o StrictHostKeyChecking=no -p %s %s@%s '%s'",
                        privateKeyFile, port, username, hostname, cmd);
            } else {
                tempPasswordFile = File.createTempFile("zstack", "tmp");
                FileUtils.writeStringToFile(tempPasswordFile, password);
                ssh = String.format("sshpass -f%s ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p %s %s@%s '%s'",
                        tempPasswordFile.getAbsolutePath(), port, username, hostname, cmd);
            }

            ShellResult ret = ShellUtils.runAndReturn(ssh);
            SshResult sret = new SshResult();
            sret.setCommandToExecute(cmd);
            sret.setReturnCode(ret.getRetCode());
            sret.setStderr(ret.getStderr());
            sret.setStdout(ret.getStdout());
            if (sret.getReturnCode() == 255) {
                sret.setSshFailure(true);
            }
            return sret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tempPasswordFile != null) {
                tempPasswordFile.delete();
            }
        }
    }

    public SshResult runScript(String script) {
        checkParams();
        String ssh;
        File tempPasswordFile = null;
        try {
            if (privateKeyFile != null) {
                ssh = ln(
                        "ssh -i {0} -o UserKnownHostsFile=/dev/null -o PasswordAuthentication=no -o StrictHostKeyChecking=no -p {1} -T {2}@{3} << 'EOF'",
                        "s=`mktemp`",
                        "cat << 'EOT' > $s",
                        "{4}",
                        "EOT",
                        "bash $s",
                        "ret=$?",
                        "rm -f $s",
                        "exit $ret",
                        "EOF"
                ).format(privateKeyFile, port, username, hostname, script);
            } else {
                tempPasswordFile = File.createTempFile("zstack", "tmp");
                FileUtils.writeStringToFile(tempPasswordFile, password);
                ssh = ln(
                        "sshpass -f{0} ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p {1} -T {2}@{3} << 'EOF'",
                        "s=`mktemp`",
                        "cat << 'EOT' > $s",
                        "{4}",
                        "EOT",
                        "bash $s",
                        "ret=$?",
                        "rm -f $s",
                        "exit $ret",
                        "EOF"
                ).format(tempPasswordFile.getAbsolutePath(), port, username, hostname, script);
            }

            ShellResult ret = ShellUtils.runAndReturn(ssh);
            SshResult sret = new SshResult();
            sret.setCommandToExecute(script);
            sret.setReturnCode(ret.getRetCode());
            sret.setStderr(ret.getStderr());
            sret.setStdout(ret.getStdout());
            if (sret.getReturnCode() == 255) {
                sret.setSshFailure(true);
            }
            return sret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tempPasswordFile != null) {
                tempPasswordFile.delete();
            }
        }
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}