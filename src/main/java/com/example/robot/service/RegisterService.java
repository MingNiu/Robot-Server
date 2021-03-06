package com.example.robot.service;

import com.example.RobotCfg;
import com.example.ServerCfg;
import com.example.robot.Client;
import com.example.rpc.EventTypes;
import com.example.rpc.Idle;
import com.example.rpc.Message;
import com.example.rpc.client.Diagnostic;
import com.example.rpc.client.DiagnosticResponse;
import com.example.rpc.server.Inform;
import com.example.rpc.server.Register;
import com.example.utils.MessageIOUtils;
import com.example.utils.PayloadUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 向Server进行注册
 */
public class RegisterService implements Runnable {
    private static final Logger logger = LogManager.getLogger(Client.class);

    @Override
    public void run() {
        while (true) {
            try (Socket socket = new Socket(ServerCfg.hostName, ServerCfg.portNumber);
                 OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {
                process(in, out);
                return;
            } catch (IOException e) {
                logger.warn("注册失败", e);
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * 处理注册流程
     * @param in
     * @param out
     * @throws IOException
     */
    private void process(InputStream in, OutputStream out) throws IOException {
        // 发Inform
        Inform inform = new Inform();
        inform.setEvent(EventTypes.BOOTSTRAP);
        Message msg = PayloadUtils.toMessage(inform);
        logger.debug("robot发送: " + msg);
        MessageIOUtils.write(out, msg);

        // 收InformResponse
        msg = MessageIOUtils.read(in);
        logger.debug("robot接收: " + msg);

        // 发Register
        Register registerReq = new Register();
        registerReq.setRobotId(RobotCfg.ROBOT_ID);
        msg = PayloadUtils.toMessage(registerReq);
        logger.debug("robot发送: " + msg);
        MessageIOUtils.write(out, msg);

        // 收RegisterResponse
        msg = MessageIOUtils.read(in);
        logger.debug("robot接收: " + msg);

        // 发Idle
        msg = PayloadUtils.toMessage(new Idle());
        logger.debug("robot发送: " + msg);
        MessageIOUtils.write(out, msg);

        // 收Diagnostic
        msg = MessageIOUtils.read(in);
        logger.debug("robot接收: " + msg);
        Diagnostic diagReq = PayloadUtils.toPayload(msg).castAs(Diagnostic.class);

        // 发DiagnosticResponse
        DiagnosticResponse diagResp = new DiagnosticResponse(diagReq.commandId);
        diagResp.setRobotId(RobotCfg.ROBOT_ID);
        msg = PayloadUtils.toMessage(diagResp);
        logger.debug("robot接收: " + msg);
        MessageIOUtils.write(out, msg);

        // 收Idle
        msg = MessageIOUtils.read(in);
        logger.debug("robot发送: " + msg);

        logger.debug("Robot注册成功");
    }
}
