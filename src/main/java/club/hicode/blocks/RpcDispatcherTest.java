package club.hicode.blocks;

import cn.hutool.core.date.DateUtil;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.*;
import org.jgroups.protocols.FRAG;
import org.jgroups.protocols.FRAG2;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.jgroups.util.RpcStats;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * @author Liu Chunfu
 * @date 2019-01-17 11:11
 **/
public class RpcDispatcherTest {

    protected RpcDispatcher da, db, dc;
    protected JChannel a, b, c;
    protected static final String GROUP = "RpcAsyncInvoker";

    // specify return values sizes which should work correctly with a 64Mb heap
    final static int[] SIZES = {10000, 20000, 40000, 80000, 100000, 200000, 400000, 800000,
            1000000, 2000000, 5000000};
    // timeout (in secs) for large value tests
    final static int LARGE_VALUE_TIMEOUT = 60;

    @Before
    public void setUp() throws Exception {
        a = createChannel("A");
        da = new RpcDispatcher(a, new ServerObject(1));
        a.connect(GROUP);

        b = createChannel("B");
        db = new RpcDispatcher(b, new ServerObject(2));
        b.connect(GROUP);

        c = createChannel("C");
        dc = new RpcDispatcher(c, new ServerObject(3));
        c.connect(GROUP);

        Util.waitUntilAllChannelsHaveSameView(10000, 1000, a, b, c);
        System.out.println("A=" + a.getView() + "\nB=" + b.getView() + "\nC=" + c.getView());
    }


    @After
    public void tearDown() throws Exception {
        Util.close(dc, db, da, c, b, a);
    }


    @Test
    public void test1(){
        System.out.println("hello");
    }

    @Test
    public void testEmptyConstructor() throws Exception {
        RpcDispatcher d1 = new RpcDispatcher();
        RpcDispatcher d2 = new RpcDispatcher();
        JChannel d =createChannel("D");
        JChannel e =createChannel("E");

        try {
            d1.setChannel(d);
            d2.setChannel(e);

            //这个是干啊嘛的？
            d1.setServerObject(new ServerObject(1));
            d2.setServerObject(new ServerObject(2));

            d1.start();
            d2.start();

            d.connect("RpcAsyncInvoker-DifferentGroup");
            e.connect("RpcAsyncInvoker-DifferentGroup");

            Util.sleep(500);

            View view = e.getView();
            System.out.println("view channel 2= " + view);

            view = d.getView();
            System.out.println("view channel 1= " + view);

            assert view.size() == 2;
            RspList<Integer> rsps = d1.callRemoteMethods(null, "foo", null, null, new RequestOptions(ResponseMode.GET_ALL, 5000));
            System.out.println("rsps:\n" + rsps);
            assert rsps.size() == 2;
            for (Rsp<Integer> rsp : rsps.values()) {
                assert rsp.wasReceived();
                assert !rsp.wasSuspected();
                assert rsp.getValue() != null;
            }


            //自己创建对象玩！
            Object server_object = new Object() {
                public long foobar() {
                    return System.currentTimeMillis();
                }
            };
            d1.setServerObject(server_object);
            d2.setServerObject(server_object);

            rsps = d2.callRemoteMethods(null, "foobar", null, null, new RequestOptions(ResponseMode.GET_ALL, 5000));
            System.out.println("rsps:\n" + rsps);
            assert rsps.size() == 2;
            for (Rsp rsp : rsps.values()) {
                assert rsp.wasReceived();
                assert !rsp.wasSuspected();
                assert rsp.getValue() != null;
            }
        } finally {
            d2.stop();
            d1.stop();
            Util.close(e, d);
        }
    }


    public void testException() throws Exception {
        RspList<Object> rsps = da.callRemoteMethods(null, "throwException", null, null, new RequestOptions(ResponseMode.GET_ALL, 5000));
        rsps.values().forEach(System.out::println);
        for (Rsp<Object> rsp : rsps.values())
            assert rsp.getException() != null && rsp.getValue() == null;
    }


    public void testExceptionAsReturnValue() throws Exception {
        RspList<Object> rsps = da.callRemoteMethods(null, "returnException", null, null, new RequestOptions(ResponseMode.GET_ALL, 5000));
        rsps.values().forEach(System.out::println);
        for (Rsp<Object> rsp : rsps.values())
            assert rsp.getException() == null && rsp.getValue() != null && rsp.getValue() instanceof Throwable;
    }

    public void testUnicastInvocation() throws Exception {
        RequestOptions opts = RequestOptions.SYNC().timeout(2000);
        Void result = da.callRemoteMethod(b.getAddress(), "bar", null, null, opts);
        assert result == null;

        Integer res = da.callRemoteMethod(b.getAddress(), "foo", null, null, opts);
        assert res != null && res == 2;
    }

    public void testUnicastInvocationWithTimeout() throws Exception {
        RequestOptions opts = RequestOptions.SYNC().timeout(1000);
        Method meth = ServerObject.class.getDeclaredMethod("sleep", long.class);
        long start = System.currentTimeMillis();
        try {
            da.callRemoteMethod(b.getAddress(), new MethodCall(meth, 5000), opts);
            assert false : "should have thrown a TimeoutException";
        } catch (TimeoutException ex) {
            long time = System.currentTimeMillis() - start;
            System.out.printf("received %s as expected; call took ~%d ms\n", ex, time);
        }
    }

    public void testUnicastInvocationWithFutureAndTimeout() throws Exception {
        RequestOptions opts = RequestOptions.SYNC().timeout(6000);
        Method meth = ServerObject.class.getDeclaredMethod("sleep", long.class);
        CompletableFuture<Long> future;
        long start = System.currentTimeMillis();
        future = da.callRemoteMethodWithFuture(b.getAddress(), new MethodCall(meth, 5000), opts);
        try {
            future.get(1000, TimeUnit.MILLISECONDS);
            assert false : "should have thrown a TimeoutException";
        } catch (TimeoutException ex) {
            long time = System.currentTimeMillis() - start;
            System.out.printf("received %s as expected; call took ~%d ms\n", ex, time);
        }
    }

    public void testUnicastInvocationWithFuture() throws Exception {
        RequestOptions opts = RequestOptions.SYNC().timeout(2000).flags(Message.Flag.OOB);
        MethodCall call = new MethodCall("bar", null, null);
        CompletableFuture<Void> future = da.callRemoteMethodWithFuture(b.getAddress(), call, opts);
        Void result = future.get(10000, TimeUnit.MILLISECONDS);
        assert result == null;

        call = new MethodCall("foo", null, null);
        CompletableFuture<Integer> fut = da.callRemoteMethodWithFuture(b.getAddress(), call, opts);
        Integer res = fut.get(10000, TimeUnit.MILLISECONDS);
        assert res != null && res == 2;

        Method meth = ServerObject.class.getDeclaredMethod("sleep", long.class);
        try {
            CompletableFuture<Long> f = da.callRemoteMethodWithFuture(b.getAddress(), new MethodCall(meth, 5000), opts);
            f.get(100, TimeUnit.MILLISECONDS);
            assert false : "should have thrown a TimeoutException";
        } catch (TimeoutException ex) {
            System.out.printf("received %s as expected\n", ex);
        }

        try {
            meth = ServerObject.class.getDeclaredMethod("throwException");
            call = new MethodCall(meth);
            CompletableFuture<Object> f = da.callRemoteMethodWithFuture(b.getAddress(), call, opts);
            f.get();
            assert false : "should have thrown ExecutionException";
        } catch (ExecutionException ex) {
            System.out.printf("received %s as expected\n", ex);
            assert ex.getCause() instanceof Exception;
        }
    }

    public void testUnicastException() {
        try {
            da.callRemoteMethod(b.getAddress(), "throwException", null, null, new RequestOptions(ResponseMode.GET_ALL, 5000));
        } catch (Throwable throwable) {
            System.out.println("received exception (as expected)");
        }
    }

    public void testUnicastExceptionNested() {
        try {
            da.callRemoteMethod(b.getAddress(), "throwExceptionNested", null, null, new RequestOptions(ResponseMode.GET_ALL, 5000));
        } catch (Throwable throwable) {
            System.out.printf("received exception (as expected): %s\n", throwable);
            assert throwable instanceof IllegalArgumentException;
            assert throwable.getCause() instanceof NullPointerException;
        }
    }

    public void testAsyncUnicast() throws Exception {
        MethodCall call = new MethodCall(ServerObject.class.getMethod("foo"));
        Integer result = da.callRemoteMethod(b.getAddress(), call, RequestOptions.ASYNC());
        assert result == null;
    }

    public void testAsyncUnicastWithFuture() throws Exception {
        MethodCall call = new MethodCall(ServerObject.class.getMethod("throwException"));
        Future<Object> future = da.callRemoteMethodWithFuture(b.getAddress(), call, RequestOptions.ASYNC());
        assert future == null;
    }


    public void testUnicastExceptionWithFuture() {
        try {
            MethodCall call = new MethodCall(ServerObject.class.getMethod("throwException"));
            Future<Object> future = da.callRemoteMethodWithFuture(b.getAddress(), call, new RequestOptions(ResponseMode.GET_ALL, 5000));
            Object val = future.get();
            assert val == null;
            assert false : " should not get here";
        } catch (Throwable throwable) {
            System.out.println("received exception (as expected): " + throwable);
        }
    }


    public void testUnicastExceptionAsReturnValue() throws Exception {
        Object rsp = da.callRemoteMethod(b.getAddress(), "returnException", null, null, new RequestOptions(ResponseMode.GET_ALL, 5000));
        System.out.println("rsp = " + rsp);
        assert rsp != null && rsp instanceof Throwable;
    }

    public void testUnicastExceptionAsReturnValueWithFuture() throws Exception {
        MethodCall call = new MethodCall(ServerObject.class.getMethod("returnException"));
        Future<Object> future = da.callRemoteMethodWithFuture(b.getAddress(), call, new RequestOptions(ResponseMode.GET_ALL, 5000));
        Object val = future.get();
        assert val instanceof Exception;
    }

    public void testMulticastInvocationWithMethodLookup() throws Exception {
        MethodCall call = new MethodCall((short) 6, 3, 4); // ServerObject.add()
        Stream.of(da, db, dc).forEach(d -> d.setMethodLookup(id -> ServerObject.methods[id]));
        RspList<Integer> rsps = da.callRemoteMethods(null, call, RequestOptions.SYNC());
        System.out.printf("rsps:\n%s\n", rsps);
        assert rsps != null;
        assert rsps.size() == 3;
        for (Rsp<Integer> rsp : rsps.values()) {
            assert rsp.getValue() != null && rsp.getValue().equals(7);
        }
    }

    public void testMulticastInvocationWithTimeout() throws Exception {
        RequestOptions opts = RequestOptions.SYNC().timeout(1000);
        Method meth = ServerObject.class.getDeclaredMethod("sleep", long.class);
        long start = System.currentTimeMillis();
        RspList<Long> rsps = da.callRemoteMethods(null, new MethodCall(meth, 5000), opts);
        long time = System.currentTimeMillis() - start;
        System.out.printf("responses:\n%s\ncall took ~%d ms\n", rsps, time);
        rsps.values().stream().allMatch(rsp -> !rsp.wasReceived());
    }

    public void testMulticastInvocationWithFutureAndTimeout() throws Exception {
        RequestOptions opts = RequestOptions.SYNC().timeout(1000);
        Method meth = ServerObject.class.getDeclaredMethod("sleep", long.class);
        CompletableFuture<RspList<Long>> future = da.callRemoteMethodsWithFuture(null, new MethodCall(meth, 5000), opts);
        RspList<Long> rsps = future.get(100, TimeUnit.MILLISECONDS);
        System.out.printf("rsps:\n%s\n", rsps);
        assert rsps != null;
        assert !rsps.values().stream().anyMatch(Rsp::wasReceived);
    }

    /**
     * Test the response filter mechanism which can be used to filter responses received with
     * a call to RpcDispatcher.
     * <p>
     * The test filters requests based on the id of the server object they were received
     * from, and only accept responses from servers with id > 1.
     * <p>
     * The expected behaviour is that the response from server 1 is rejected, but the responses
     * from servers 2 and 3 are accepted.
     */
    public void testResponseFilter() throws Exception {
        RequestOptions options = new RequestOptions(ResponseMode.GET_ALL, 10000, false,
                new RspFilter() {
                    int num = 0;

                    @Override
                    public boolean isAcceptable(Object response, Address sender) {
                        boolean retval = (Integer) response > 1;
                        if (retval)
                            num++;
                        return retval;
                    }

                    @Override
                    public boolean needMoreResponses() {
                        return num < 2;
                    }
                });

        RspList rsps = da.callRemoteMethods(null, "foo", null, null, options);
        System.out.println("responses are:\n" + rsps);
        assert rsps.size() == 3 : "there should be three response values";
        assert rsps.numReceived() == 2 : "number of responses received should be 2";
    }


    /**
     * Test a unicast blocking RPC with a stupid response filter which never terminates
     */
    public void testResponseFilterWithUnicast() throws Exception {
        RequestOptions options = RequestOptions.SYNC().timeout(5000).rspFilter(
                new RspFilter() {
                    public boolean isAcceptable(Object response, Address sender) {
                        return false;
                    }

                    public boolean needMoreResponses() {
                        return true;
                    }
                });

        Object retval = da.callRemoteMethod(b.getAddress(), "bar", null, null, options);
        System.out.println("retval = " + retval);
        assert retval == null;
    }


    /**
     * Tests an incorrect response filter which always returns false for isAcceptable() and true for needsMoreResponses().
     * The call should return anyway after having received all responses, even if none of them was accepted by the
     * filter.
     */
    public void testNonTerminatingResponseFilter() throws Exception {
        RequestOptions options = new RequestOptions(ResponseMode.GET_ALL, 10000, false,
                new RspFilter() {
                    @Override
                    public boolean isAcceptable(Object response, Address sender) {
                        return false;
                    }
                    @Override
                    public boolean needMoreResponses() {
                        return true;
                    }
                });

        RspList rsps = da.callRemoteMethods(null, "foo", null, null, options);
        System.out.println("responses are:\n" + rsps);
        Util.assertEquals("there should be three response values", 3, rsps.size());
        Util.assertEquals("number of responses received should be 3", 0, rsps.numReceived());
    }

    /**
     * Runs with response mode of GET_FIRST and the response filter accepts only the last response
     *
     * @throws Exception
     */
    public void testAcceptLastResponseFilter() throws Exception {
        RequestOptions options = new RequestOptions(ResponseMode.GET_FIRST, 10000, false,
                new RspFilter() {
                    int count = 0;

                    public boolean isAcceptable(Object response, Address sender) {
                        return ++count >= 3;
                    }

                    public boolean needMoreResponses() {
                        return count < 3;
                    }
                });

        RspList rsps = da.callRemoteMethods(null, "foo", null, null, options);
        System.out.println("responses are:\n" + rsps);
        Util.assertEquals("there should be three response values", 3, rsps.size());
        Util.assertEquals("number of responses received should be 3", 1, rsps.numReceived());
    }


    public void testFuture() throws Exception {
        MethodCall sleep = new MethodCall("sleep", new Object[]{5000L}, new Class[]{long.class});
        CompletableFuture<RspList<Long>> future = da.callRemoteMethodsWithFuture(null, sleep,
                new RequestOptions(ResponseMode.GET_ALL, 5000L, false, null));
        assert !future.isDone();
        assert !future.isCancelled();

        RspList<Long> rsps = future.get(300, TimeUnit.MILLISECONDS);
        long num_not_received = rsps.values().stream().filter(rsp -> !rsp.wasReceived()).count();
        System.out.printf("rsps:\n%s\nnot received: %d\n", rsps, num_not_received);
        assert rsps.size() == 3;
        assert num_not_received == 3 : "none of the 3 requests should have received a response, rsps:\n" + rsps;
        assert future.isDone();
    }


    public void testNotifyingFuture() throws Exception {
        MethodCall sleep = new MethodCall("sleep", new Object[]{1000L}, new Class[]{long.class});
        CompletableFuture<RspList<Long>> future = da.callRemoteMethodsWithFuture(null, sleep, new RequestOptions(ResponseMode.GET_ALL, 5000L));
        assert !future.isDone();
        assert !future.isCancelled();
        for (int i = 0; i < 10; i++) {
            if (future.isDone())
                break;
            Util.sleep(1000);
        }
        assert future.isDone();
        RspList<Long> result = future.get(1L, TimeUnit.MILLISECONDS);
        System.out.println("result:\n" + result);
        assert result != null;
        assert result.size() == 3;
        assert future.isDone();

        RspList<Long> result2 = future.get();
        System.out.println("result2:\n" + result2);
        assert result2 != null;
        assert result2.size() == 3;
        assert future.isDone();
    }

    public void testNotifyingFutureWithDelayedListener() throws Exception {
        MethodCall sleep = new MethodCall("sleep", new Object[]{100L}, new Class[]{long.class});
        CompletableFuture<RspList<Long>> future = da.callRemoteMethodsWithFuture(null, sleep, new RequestOptions(ResponseMode.GET_ALL, 5000L));
        assert !future.isDone();
        assert !future.isCancelled();

        Util.sleep(2000);
        assert future.isDone();
        RspList result = future.get(1L, TimeUnit.MILLISECONDS);
        System.out.println("result:\n" + result);
        assert result != null;
        assert result.size() == 3;
    }


    /**
     * Invoke a call which sleeps for 5s 5 times. Since the sleep should be done in parallel (OOB msgs), all 5 futures
     * should be done in roughly 5s. JIRA: https://issues.jboss.org/browse/JGRP-2039
     */
    public void testMultipleFutures() throws Exception {
        final int NUM_CALLS = 5, MAX_SLEEP = 10000; // should be done in ~5s, make it 10 to be on the safe side
        MethodCall sleep = new MethodCall("sleep", new Object[]{5000L}, new Class[]{long.class});
        List<Future<RspList<Long>>> futures = new ArrayList<>();
        long target = System.currentTimeMillis() + MAX_SLEEP;

        // if we didn't use DONT_BUNDLE, all OOB msgs from the same sender in a batch would be delivered sequentially!
        RequestOptions options = new RequestOptions(ResponseMode.GET_ALL, 30000L)
                .flags(Message.Flag.OOB, Message.Flag.DONT_BUNDLE);
        for (int i = 0; i < NUM_CALLS; i++) {
            Future<RspList<Long>> future = da.callRemoteMethodsWithFuture(null, sleep, options);
            futures.add(future);
        }

        List<Future<RspList<Long>>> rsps = new ArrayList<>();
        while (!futures.isEmpty() && System.currentTimeMillis() < target) {
            for (Iterator<Future<RspList<Long>>> it = futures.iterator(); it.hasNext(); ) {
                Future<RspList<Long>> future = it.next();
                if (future.isDone()) {
                    it.remove();
                    rsps.add(future);
                }
            }
            Util.sleep(500);
        }
        System.out.println("\n" + rsps.size() + " responses:\n");
        rsps.forEach(System.out::println);
        assert rsps.size() == NUM_CALLS;
    }

    public void testMultipleNotifyingFutures() throws Exception {
        MethodCall sleep = new MethodCall("sleep", new Object[]{100L}, new Class[]{long.class});
        List<CompletableFuture<RspList<Long>>> listeners = new ArrayList<>();
        RequestOptions options = new RequestOptions(ResponseMode.GET_ALL, 30000L);
        for (int i = 0; i < 10; i++) {
            CompletableFuture<RspList<Long>> f = da.callRemoteMethodsWithFuture(null, sleep, options);
            listeners.add(f);
        }

        Util.sleep(1000);
        for (int i = 0; i < 10; i++) {
            boolean all_done = true;
            for (CompletableFuture<RspList<Long>> listener : listeners) {
                boolean done = listener.isDone();
                System.out.print(done ? "+ " : "- ");
                if (!listener.isDone())
                    all_done = false;
            }
            if (all_done)
                break;
            Util.sleep(500);
            System.out.println("");
        }
        for (CompletableFuture<RspList<Long>> listener : listeners)
            assert listener.isDone();
    }


    public void testFutureCancel() throws Exception {
        MethodCall sleep = new MethodCall("sleep", new Object[]{1000L}, new Class[]{long.class});
        Future<RspList<Long>> future = da.callRemoteMethodsWithFuture(null, sleep, new RequestOptions(ResponseMode.GET_ALL, 5000L));
        assert !future.isDone();
        assert !future.isCancelled();
        future.cancel(true);
        assert future.isDone();
        assert future.isCancelled();

        future = da.callRemoteMethodsWithFuture(null, sleep, new RequestOptions(ResponseMode.GET_ALL, 0));
        assert !future.isDone();
        assert !future.isCancelled();
        future.cancel(true);
        assert future.isDone();
        assert future.isCancelled();
    }


    /**
     * Test the ability of RpcDispatcher to handle large argument and return values
     * with multicast RPC calls.
     * <p>
     * The test sends requests for return values (byte arrays) having increasing sizes,
     * which increase the processing time for requests as well as the amount of memory
     * required to process requests.
     * <p>
     * The expected behaviour is that all RPC requests complete successfully.
     */
    public void testLargeReturnValue() throws Exception {
        setProps(a, b, c);
        for (int i = 0; i < SIZES.length; i++) {
            _testLargeValue(SIZES[i]);
        }
    }


    /**
     * Tests a method call to {A,B,C} where C left *before* the call. http://jira.jboss.com/jira/browse/JGRP-620
     */
    public void testMethodInvocationToNonExistingMembers() throws Exception {
        final int timeout = 5 * 1000;

        // get the current membership, as seen by C
        View view = c.getView();
        List<Address> members = view.getMembers();
        System.out.println("list is " + members);

        // cause C to leave the group and close its channel
        System.out.println("closing c3");
        c.close();

        Util.sleep(1000);

        // make an RPC call using C's now outdated view of membership
        System.out.println("calling method foo() in " + members + " (view=" + b.getView() + ")");
        RspList<Integer> rsps = da.callRemoteMethods(members, "foo", null, null, new RequestOptions(ResponseMode.GET_ALL, timeout));

        // all responses
        System.out.println("responses:\n" + rsps);
        assert rsps.size() == 2;
        for (Map.Entry<Address, Rsp<Integer>> entry : rsps.entrySet()) {
            Rsp rsp = entry.getValue();
            Util.assertTrue("response from " + entry.getKey() + " was received", rsp.wasReceived());
            Util.assertFalse(rsp.wasSuspected());
        }

        List<Address> mbrs = new ArrayList<>(members);
        mbrs.remove(b.getAddress());
        System.out.println("calling method foo() in " + mbrs + " (view=" + b.getView() + ")");
        rsps = da.callRemoteMethods(mbrs, "foo", null, null, new RequestOptions(ResponseMode.GET_ALL, timeout));

        // all responses
        System.out.println("responses:\n" + rsps);
        assert rsps.size() == 1;
        for (Map.Entry<Address, Rsp<Integer>> entry : rsps.entrySet()) {
            Rsp rsp = entry.getValue();
            Util.assertTrue("response from " + entry.getKey() + " was received", rsp.wasReceived());
            Util.assertFalse(rsp.wasSuspected());
        }

        rsps = da.callRemoteMethods(mbrs, "foo", null, null,
                new RequestOptions(ResponseMode.GET_ALL, timeout).transientFlags(Message.TransientFlag.DONT_LOOPBACK));


        System.out.println("responses:\n" + rsps);
        assert rsps.isEmpty();

        mbrs.clear();
        rsps = da.callRemoteMethods(mbrs, "foo", null, null,
                new RequestOptions(ResponseMode.GET_ALL, timeout).transientFlags(Message.TransientFlag.DONT_LOOPBACK));

        // all responses
        System.out.println("responses:\n" + rsps);
        assert rsps.isEmpty();
    }


    /**
     * Test the ability of RpcDispatcher to handle large argument and return values
     * with unicast RPC calls.
     * <p>
     * The test sends requests for return values (byte arrays) having increasing sizes,
     * which increase the processing time for requests as well as the amount of memory
     * required to process requests.
     * <p>
     * The expected behaviour is that all RPC requests complete successfully.
     */
    public void testLargeReturnValueUnicastCall() throws Exception {
        setProps(a, b, c);
        for (int i = 0; i < SIZES.length; i++) {
            _testLargeValueUnicastCall(a.getAddress(), SIZES[i]);
        }
    }

    public void testRpcStats() throws Exception {
        Method meth = ServerObject.class.getDeclaredMethod("foo");
        List<Address> targets = Arrays.asList(b.getAddress(), c.getAddress());
        RpcStats stats = da.rpcStats().extendedStats(true);

        // sync mcast with future
        da.callRemoteMethodsWithFuture(null, new MethodCall(meth), RequestOptions.SYNC());
        System.out.println("stats = " + stats);
        assert stats.multicasts(true) == 1;

        // async mcast with future
        da.callRemoteMethodsWithFuture(null, new MethodCall(meth), RequestOptions.ASYNC());
        System.out.println("stats = " + stats);
        assert stats.multicasts(false) == 1;

        // sync anycast with future
        da.callRemoteMethodsWithFuture(targets, new MethodCall(meth), RequestOptions.SYNC().anycasting(true));
        System.out.println("stats = " + stats);
        assert stats.anycasts(true) == 1;

        // async anycast with future
        da.callRemoteMethodsWithFuture(targets, new MethodCall(meth), RequestOptions.ASYNC().anycasting(true));
        System.out.println("stats = " + stats);
        assert stats.anycasts(false) == 1;

        // sync unicast with future
        da.callRemoteMethodWithFuture(b.getAddress(), new MethodCall(meth), RequestOptions.SYNC());
        System.out.println("stats = " + stats);
        assert stats.unicasts(true) == 1;

        // async unicast with future
        da.callRemoteMethodWithFuture(b.getAddress(), new MethodCall(meth), RequestOptions.ASYNC());
        System.out.println("stats = " + stats);
        assert stats.unicasts(false) == 1;


        // sync mcast
        da.callRemoteMethods(null, new MethodCall(meth), RequestOptions.SYNC());
        System.out.println("stats = " + stats);
        assert stats.multicasts(true) == 2;

        // async mcast
        da.callRemoteMethods(null, new MethodCall(meth), RequestOptions.ASYNC());
        System.out.println("stats = " + stats);
        assert stats.multicasts(false) == 2;

        // sync anycast
        da.callRemoteMethods(targets, new MethodCall(meth), RequestOptions.SYNC().anycasting(true));
        System.out.println("stats = " + stats);
        assert stats.anycasts(true) == 2;

        // async anycast
        da.callRemoteMethods(targets, new MethodCall(meth), RequestOptions.ASYNC().anycasting(true));
        System.out.println("stats = " + stats);
        assert stats.anycasts(false) == 2;

        // sync unicast
        da.callRemoteMethod(b.getAddress(), new MethodCall(meth), RequestOptions.SYNC());
        System.out.println("stats = " + stats);
        assert stats.unicasts(true) == 2;

        // async unicast
        da.callRemoteMethod(b.getAddress(), new MethodCall(meth), RequestOptions.ASYNC());
        System.out.println("stats = " + stats);
        assert stats.unicasts(false) == 2;
    }


    protected static void setProps(JChannel... channels) {
        for (JChannel ch : channels) {
            Protocol prot = ch.getProtocolStack().findProtocol(FRAG2.class);
            if (prot != null) {
                ((FRAG2) prot).setFragSize(12000);
            }
            prot = ch.getProtocolStack().findProtocol(FRAG.class);
            if (prot != null) {
                ((FRAG) prot).setFragSize(12000);
            }

            prot = ch.getProtocolStack().getTransport();
            if (prot != null)
                ((TP) prot).setMaxBundleSize(14000);
        }
    }

    protected JChannel createChannel(String name) throws Exception {
        return new JChannel(Util.getTestStack()).name(name);
    }


    /**
     * Helper method to perform a RPC call on server method "returnValue(int size)" for
     * all group members.
     * <p>
     * The method checks that each returned value is non-null and has the correct size.
     */
    void _testLargeValue(int size) throws Exception {

        final long timeout = LARGE_VALUE_TIMEOUT * 1000;

        System.out.println("\ntesting with " + size + " bytes");
        long startTime = System.currentTimeMillis();
        RspList<Object> rsps = da.callRemoteMethods(null, "largeReturnValue", new Object[]{size}, new Class[]{int.class},
                new RequestOptions(ResponseMode.GET_ALL, timeout));
        long stopTime = System.currentTimeMillis();
        System.out.println("test took: " + (stopTime - startTime) + " ms");
        System.out.println("rsps:");
        assert rsps.size() == 3 : "there should be three responses to the RPC call but only " + rsps.size() +
                " were received: " + rsps;

        for (Map.Entry<Address, Rsp<Object>> entry : rsps.entrySet()) {

            // its possible that an exception was raised in processing
            Object obj = entry.getValue().getValue();

            // this should not happen
            assert !(obj instanceof Throwable) : "exception was raised in processing reasonably sized argument";

            byte[] val = (byte[]) obj;
            assert val != null;
            System.out.println(val.length + " bytes from " + entry.getKey());
            assert val.length == size : "return value does not match required size";
        }
    }

    /**
     * Helper method to perform a RPC call on server method "returnValue(int size)" for
     * all group members.
     * <p>
     * This method need to take into account that RPC calls can timeout with huge values,
     * and they can also trigger OOMEs. But if we are lucky, they can also return
     * reasonable values.
     */
    void _testHugeValue(int size) throws Exception {

        // 20 second timeout
        final long timeout = 20 * 1000;

        System.out.println("\ntesting with " + size + " bytes");
        RspList<Object> rsps = da.callRemoteMethods(null, "largeReturnValue", new Object[]{size}, new Class[]{int.class},
                new RequestOptions(ResponseMode.GET_ALL, timeout));
        System.out.println("rsps:");
        assert rsps != null;
        assert rsps.size() == 3 : "there should be three responses to the RPC call but only " + rsps.size() +
                " were received: " + rsps;

        // in checking the return values, we need to take account of timeouts (i.e. when
        // a null value is returned) and exceptions
        for (Map.Entry<Address, Rsp<Object>> entry : rsps.entrySet()) {

            Object obj = entry.getValue().getValue();

            // its possible that an exception was raised
            if (obj instanceof java.lang.Throwable) {
                Throwable t = (Throwable) obj;

                System.out.println(t.toString() + " exception was raised processing argument from " +
                        entry.getKey() + " -this is expected");
                continue;
            }

            // its possible that the request timed out before the serve could reply
            if (obj == null) {
                System.out.println("request timed out processing argument from " +
                        entry.getKey() + " - this is expected");
                continue;
            }

            // if we reach here, we sould have a reasonable value
            byte[] val = (byte[]) obj;
            System.out.println(val.length + " bytes from " + entry.getKey());
            assert val.length == size : "return value does not match required size";
        }
    }

    /**
     * Helper method to perform a RPC call on server method "returnValue(int size)" for
     * an individual group member.
     * <p>
     * The method checks that the returned value is non-null and has the correct size.
     *
     * @param dst  the group member
     * @param size the size of the byte array to be returned
     */
    void _testLargeValueUnicastCall(Address dst, int size) throws Exception {

        final long timeout = LARGE_VALUE_TIMEOUT * 1000;

        System.out.println("\ntesting unicast call with " + size + " bytes");
        Util.assertNotNull(dst);

        long startTime = System.currentTimeMillis();
        byte[] val = da.callRemoteMethod(dst, "largeReturnValue", new Object[]{size}, new Class[]{int.class},
                new RequestOptions(ResponseMode.GET_ALL, timeout));
        long stopTime = System.currentTimeMillis();
        System.out.println("test took: " + (stopTime - startTime) + " ms");

        // check value is not null, otherwise fail the test
        Util.assertNotNull("return value should be non-null", val);
        System.out.println("rsp: " + val.length + " bytes");

        // returned value should have requested size
        Util.assertEquals("return value does not match requested size", size, val.length);
    }

    /**
     * This class serves as a server obect to turn requests into replies.
     * It is initialised with an integer id value.
     * <p>
     * It implements two functions:
     * function foo() returns the id of the server
     * function largeReturnValue(int size) returns a byte array of size 'size'
     */
    protected static class ServerObject {
        protected int i;

        protected static final Method[] methods;

        static {
            try {
                methods = new Method[]{
                        ServerObject.class.getDeclaredMethod("foo"), // index 0
                        ServerObject.class.getDeclaredMethod("bar"),
                        ServerObject.class.getDeclaredMethod("sleep", long.class),
                        ServerObject.class.getDeclaredMethod("throwException"),
                        ServerObject.class.getDeclaredMethod("returnException"),
                        ServerObject.class.getDeclaredMethod("largeReturnValue", int.class),
                        ServerObject.class.getDeclaredMethod("add", int.class, int.class) // index 6
                };
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }


        public ServerObject(int i) {
            this.i = i;
        }

        public int foo() {
            System.out.println("我被调用了。。。"+ DateUtil.now());
            return i;
        }

        public int foo2(String no) {
            System.out.println("我被"+no+"调用了。。。"+ DateUtil.now());
            return i;
        }

        public static void bar() {
            ;
        }

        public static long sleep(long timeout) {
            long start = System.currentTimeMillis();
            Util.sleep(timeout);
            return System.currentTimeMillis() - start;
        }


        public static void throwException() throws Exception {
            throw new Exception("booom");
        }

        public static Exception returnException() {
            return new Exception("booom");
        }

        public static byte[] largeReturnValue(int size) {
            return new byte[size];
        }

        public static int add(int a, int b) {
            return a + b;
        }

        public static void throwExceptionNested() throws Exception {
            Exception ex = new IllegalArgumentException("illegal argument - see cause for details");
            Exception cause = new NullPointerException("the arg was null!");
            ex.initCause(cause);
            throw ex;
        }

    }

}