package io.github.quickmsg.edge.mqtt.topic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author luxurong
 */

public class TopicTreeNode<T> {

    public  final Set<String> topicCollect;
    private final String topic;

    private Set<T> objects;

    private AtomicLong atomicLong = new AtomicLong(0);

    private Map<String, TopicTreeNode<T>> childNodes = new ConcurrentHashMap<>();

    public TopicTreeNode(Set<String> topicCollect, String topic) {
        this.topicCollect = topicCollect;
        this.topic = topic;
        this.objects = new CopyOnWriteArraySet<>();
    }


    private final String ONE_SYMBOL = "+";

    private final String MORE_SYMBOL = "#";

    public boolean addObjectTopic(String topic, T t) {
        String[] topics = topic.split("/");
        boolean result = addIndex(t, topics, 0);
        if (result) {
            this.topicCollect.add(topic);
            atomicLong.incrementAndGet();
        }
        return result;
    }


    private boolean addTreeObject(T t) {
        return objects.add(t);
    }


    private boolean addIndex(T t, String[] topics, Integer index) {
        String lastTopic = topics[index];
        TopicTreeNode<T> treeNode = childNodes.computeIfAbsent(lastTopic, topic ->
                new TopicTreeNode<>(topicCollect,topic));
        if (index == topics.length - 1) {
            return treeNode.addTreeObject(t);
        } else {
            return treeNode.addIndex(t, topics, index + 1);
        }
    }


    public Set<T> getObjectsByTopic(String topicFilter) {
        String[] topics = topicFilter.split("/");
        return searchTree(topics);
    }


    private Set<T> searchTree(String[] topics) {
        HashSet<T> objectList = new HashSet<>();
        loadTreeObjects(this, objectList, topics, 0);
        return objectList;
    }

    private void loadTreeObjects(TopicTreeNode<T> treeNode, HashSet<T> objects, String[] topics, Integer index) {
        String lastTopic = topics[index];
        TopicTreeNode<T> moreTreeNode = treeNode.getChildNodes().get(MORE_SYMBOL);
        if (moreTreeNode != null) {
            objects.addAll(moreTreeNode.getObjects());
        }
        if (index == topics.length - 1) {
            TopicTreeNode<T> localTreeNode = treeNode.getChildNodes().get(lastTopic);
            if (localTreeNode != null) {
                Set<T> lists = localTreeNode.getObjects();
                if (lists != null && !lists.isEmpty()) {
                    objects.addAll(lists);
                }
            }
            localTreeNode = treeNode.getChildNodes().get(ONE_SYMBOL);
            if (localTreeNode != null) {
                Set<T> lists = localTreeNode.getObjects();
                if (lists != null && !lists.isEmpty()) {
                    objects.addAll(lists);
                }
            }

        } else {
            TopicTreeNode<T> oneTreeNode = treeNode.getChildNodes().get(ONE_SYMBOL);
            if (oneTreeNode != null) {
                loadTreeObjects(oneTreeNode, objects, topics, index + 1);
            }
            TopicTreeNode<T> node = treeNode.getChildNodes().get(lastTopic);
            if (node != null) {
                loadTreeObjects(node, objects, topics, index + 1);
            }
            TopicTreeNode<T> moreNode = treeNode.getChildNodes().get(MORE_SYMBOL);
            if (moreNode != null) {
                loadTreeObjects(moreNode, objects, topics, index + 1);
            }
        }

    }

    public boolean removeObjectTopic(String topicFilter, T t) {
        TopicTreeNode<T> node = this;
        String[] topics = topicFilter.split("/");
        for (String topic : topics) {
            if (node != null) {
                node = node.getChildNodes().get(topic);
            }
        }
        if (node != null) {
            Set<T> subscribeTopics = node.getObjects();
            if (subscribeTopics != null) {
                boolean result = subscribeTopics.remove(t);
                if (result) {
                    if (subscribeTopics.isEmpty()) {
                        this.topicCollect.remove(topicFilter);
                    }
                    atomicLong.decrementAndGet();
                }
                return result;
            }
        }
        return false;
    }

    public Set<T> getAllObjectsTopic() {
        return getTreeObjectsTopic(this);
    }

    private Set<T> getTreeObjectsTopic(TopicTreeNode<T> node) {
        Set<T> allSubscribeTopics = new HashSet<>();
        allSubscribeTopics.addAll(node.getObjects());
        allSubscribeTopics.addAll(node.getChildNodes()
                .values()
                .stream()
                .flatMap(treeNode -> treeNode.getTreeObjectsTopic(treeNode).stream())
                .collect(Collectors.toSet()));
        return allSubscribeTopics;
    }

    public long size() {
        return atomicLong.get();
    }

    public Set<String> getTopicCollect() {
        return topicCollect;
    }

    public String getTopic() {
        return topic;
    }

    public Set<T> getObjects() {
        return objects;
    }

    public void setObjects(Set<T> objects) {
        this.objects = objects;
    }

    public AtomicLong getAtomicLong() {
        return atomicLong;
    }

    public void setAtomicLong(AtomicLong atomicLong) {
        this.atomicLong = atomicLong;
    }

    public Map<String, TopicTreeNode<T>> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(Map<String, TopicTreeNode<T>> childNodes) {
        this.childNodes = childNodes;
    }

    public String getONE_SYMBOL() {
        return ONE_SYMBOL;
    }

    public String getMORE_SYMBOL() {
        return MORE_SYMBOL;
    }
}
