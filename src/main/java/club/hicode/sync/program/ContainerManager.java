package club.hicode.sync.program;

import java.util.ArrayList;
import java.util.List;

/**
 * 容器管理器
 *
 * @author Liu Chunfu
 * @date 2019-01-18 10:03
 **/
public class ContainerManager {

    private List<String> container = new ArrayList<>();

    public void setContainer(List<String> ori) {
        this.container = ori;
        System.err.println("重新赋值成功！");
        System.err.println(container);
        System.err.println("========");
    }

}
