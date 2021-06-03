/*******************************************************************************
 * Copyright 2014-2017 Specure GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package at.rtr.rmbt.client;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import at.rtr.rmbt.client.v2.task.AbstractQoSTask;
import at.rtr.rmbt.client.v2.task.QoSTestEnum;
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector;
import at.rtr.rmbt.client.v2.task.result.QoSTestResult;
import at.rtr.rmbt.client.v2.task.service.TestSettings;
import at.rtr.rmbt.client.v2.task.service.TrafficService;

/*
 * This is implementation of the VOIP test possible to run with themain test
 */

public class JitterTest extends VoipTest {

    RMBTClient client;

    public JitterTest(RMBTClient client, TestSettings nnTestSettings) {
        super(client, nnTestSettings, true, null, true);
        this.client = client;
    }

    @Override
    protected String getTestId() {
//        TODO: return back when JITTER will be done
//        return RMBTClient.TASK_JITTER;
        return RMBTClient.TASK_VOIP;
    }


    @Override
    public QoSResultCollector call() throws Exception {
        status.set(QoSTestEnum.VOIP);
        QoSResultCollector result = new QoSResultCollector();

        final int testSize = testCount.get();

        int trafficServiceStatus = TrafficService.SERVICE_NOT_SUPPORTED;

        if (qoSTestSettings != null && qoSTestSettings.getTrafficService() != null) {
            trafficServiceStatus = qoSTestSettings.getTrafficService().start();
        }

        Iterator<Integer> groupIterator = concurrentTasks.keySet().iterator();
        while (groupIterator.hasNext() && !status.get().equals(QoSTestEnum.ERROR)) {
            final int groupId = groupIterator.next();
            concurrentGroupCount.set(groupId);

            //check if a qos control server connection needs to be initialized:
            openControlConnections(groupId);

            if (status.get().equals(QoSTestEnum.ERROR)) {
                break;
            }

            List<AbstractQoSTask> tasks = concurrentTasks.get(groupId);
            for (AbstractQoSTask task : tasks) {
                executorService.submit(task);
            }

            for (int i = 0; i < tasks.size(); i++) {
                try {
                    Future<QoSTestResult> testResult = executorService.take();
                    if (testResult != null) {
                        QoSTestResult curResult = testResult.get();

                        if (curResult.isFatalError()) {
                            throw new InterruptedException("interrupted due to test fatal error: " + curResult.toString());
                        }

                        if (!curResult.getQosTask().hasConnectionError()) {
                            result.getResults().add(curResult);
                        } else {
                            System.out.println("test: " + curResult.getTestType().name() + " failed. Could not connect to QoSControlServer.");
                        }
                        System.out.println("test " + curResult.getTestType().name() + " finished (" + (progress.get() + 1) + " out of " +
                                testSize + ", CONCURRENCY GROUP=" + groupId + ")");
                        QualityOfServiceTest.Counter testTypeCounter = testGroupCounterMap.get(curResult.getTestType());
                        if (testTypeCounter != null) {
                            testTypeCounter.value++;
                        }
                    }

                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    e.printStackTrace();
                    status.set(QoSTestEnum.ERROR);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    progress.incrementAndGet();
                }
            }

            closeControlConnections(groupId);
        }

        if (status.get().equals(QoSTestEnum.ERROR)) {
            progress.set(testCount.get());
        }

        if (trafficServiceStatus != TrafficService.SERVICE_NOT_SUPPORTED) {
            qoSTestSettings.getTrafficService().stop();
            System.out.println("TRAFFIC SERVICE: Tx Bytes = " + qoSTestSettings.getTrafficService().getTxBytes()
                    + ", Rx Bytes = " + qoSTestSettings.getTrafficService().getRxBytes());
        }

        if (status.get() != QoSTestEnum.ERROR) {
            status.set(QoSTestEnum.QOS_FINISHED);
        }

        if (executor != null)
            executor.shutdownNow();

        return result;
    }

    /**
     * @return
     */
    public RMBTClient getRMBTClient() {

        return client;
    }
}
