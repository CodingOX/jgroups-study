package club.hicode.state;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import org.jgroups.*;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 状态回收器
 *
 * @author Liu Chunfu
 * @date 2019-01-17 17:03
 **/
public class StateReceiver extends ReceiverAdapter {


    private static final String FLAG = "mic-state";

    private List<String> container = new ArrayList<>();

    private JChannel channel;

    @Override
    public void receive(Message msg) {
        System.out.println("获取到消息:" + msg.getObject());
        container.add(msg.getObject());
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (FLAG) {
            //触发
            System.out.println("触发状态1");
            Util.objectToStream(container, new DataOutputStream(output));
        }
    }


    @Override
    public void setState(InputStream input) throws Exception {
        synchronized (FLAG) {
            //触发
            System.out.println("触发状态2");
            Object o = Util.objectFromStream(new DataInputStream(input));
            List<String> result = Convert.convert(new TypeReference<List<String>>() {
            }, o);
            container = result;

            System.out.println("====更新已更新===" + container);
        }
    }

    @Override
    public void viewAccepted(View view) {
        System.err.println("新成员加入：" + view);
    }


    public void start() throws Exception {
        channel = new JChannel();
        //channel.receiver(this::receive);
        channel.setReceiver(this);
        channel.connect(FLAG);

        //必须添加此句才能够正常获取到状态
        channel.getState(null, 10000);
        //System.out.println("------");
        //channel.close();
    }

    public static void main(String[] args) throws Exception {
        StateReceiver receive = new StateReceiver();
        receive.start();
    }

}
