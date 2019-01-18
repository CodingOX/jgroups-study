package club.hicode.state;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * x
 *
 * @author Liu Chunfu
 * @date 2019-01-17 14:27
 **/
public class SimpleChat extends ReceiverAdapter {

    /**
     * 锁
     */
    private Object block = new Object();

    JChannel channel;

    String user_name = System.getProperty("user.name", "n/a");

    /**
     * 内容集合，同我想象中的 container 很像！
     */
    final List<String> state = new LinkedList<>();

    /**
     * 最开始的视图构建
     *
     * @param new_view
     */
    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    /**
     * 接收消息
     *
     * @param msg
     */
    @Override
    public void receive(Message msg) {
        //msg.getSrc 是发起者的标识。 msg.getObject 获取的是内容
        String line = msg.getSrc() + ": " + msg.getObject();
        System.out.println(line);
        synchronized (block) {
            state.add(line);
        }
    }

    /**
     * 获取共享数据的状态
     *
     * @param output
     * @throws Exception
     */
    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (block) {
            Util.objectToStream(state, new DataOutputStream(output));
            //如果将大量较小的片段写入输出流，则最好将输出流包装到BufferedOutputStream中，例如
            //Util.objectToStream(state, new BufferedOutputStream(new DataOutputStream(output),));
        }
    }

    /**
     * 设置共享数据的状态
     *
     * @param input
     * @throws Exception
     */
    @Override
    public void setState(InputStream input) throws Exception {
        List<String> list = Util.objectFromStream(new DataInputStream(input));
        synchronized (block) {
            state.clear();
            state.addAll(list);
        }
        System.out.println("received state (" + list.size() + " messages in chat history):");
        list.forEach(System.out::println);
    }


    private void start() throws Exception {
        //之所以为 this 是因为当前类实现了接口 ReceiverAdapter
        channel = new JChannel().setReceiver(this);
        channel.connect("ChatCluster");
        channel.getState(null, 10000);
        eventLoop();
        channel.close();
    }

    private void eventLoop() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("> ");
                System.out.flush();
                String line = in.readLine().toLowerCase();
                if (line.startsWith("quit") || line.startsWith("exit")) {
                    break;
                }
                line = "[" + user_name + "] " + line;
                //创建消息
                Message msg = new Message(null, line);
                //通过通道进行发送
                channel.send(msg);
            } catch (Exception e) {
            }
        }
    }


    public static void main(String[] args) throws Exception {
        new SimpleChat().start();
    }
}