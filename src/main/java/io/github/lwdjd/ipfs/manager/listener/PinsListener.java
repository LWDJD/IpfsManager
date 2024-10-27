package io.github.lwdjd.ipfs.manager.listener;

public interface PinsListener {
    /**
     * 使用AutoPins.sendPins()方法过程中监听每个请求的返回结果
     * @param total 需要请求CID的总数量
     * @param pinned 当前队列位置
     * @param cid 当前CID
     * @param report 请求结果
     */
    void onPins(int total,int pinned,String cid,String fileName,String report);
}
