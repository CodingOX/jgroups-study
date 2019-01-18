package club.hicode.sync.program;

import cn.hutool.core.thread.ThreadUtil;
import com.github.jsonzou.jmockdata.JMockData;
import com.github.jsonzou.jmockdata.TypeReference;

import java.util.ArrayList;

/**
 * 调用 invoker
 *
 * @author Liu Chunfu
 * @date 2019-01-18 10:16
 **/
public class RpcInvoker {

    public static void main(String[] args) throws Exception {
        ContainerSync sync = new ContainerSync();
        //for (int i = 0; i < 100; i++) {
        //    ArrayList<String> mock = JMockData.mock(new TypeReference<ArrayList<String>>() {
        //    });
        //    sync.invokeRemote(mock);
        //    System.out.println("第" + i + "调用完成。。");
        //    ThreadUtil.sleep(2000);
        //}
    }
}
