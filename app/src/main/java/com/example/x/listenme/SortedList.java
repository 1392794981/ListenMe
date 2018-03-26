package com.example.x.listenme;

/**
 * Created by X on 2018/3/25.
 */

public class SortedList {
    public static class Node {
        double value;
        String data;
        Node next = null;

        Node(double v, String d) {
            value = v;
            data = d;
        }
    }

    public Node first;
    public int position;

    public SortedList() {
        first = null;
        position = 0;
    }

    public int insertByOrder(double value) {
        return insertByOrder(value,null);
    }

    public int insertByOrder(double value, String data) {
        int pos = isExist(value);
        if (pos >= 0) return pos;

        Node newNode = new Node(value,data);
        pos = 0;

        if (first == null) {
            first = newNode;
            return pos;
        } else {
            Node currentNode = first;
            while (currentNode.next != null) {
                Node nextNode = currentNode.next;
                if (nextNode.value >= value) {
                    newNode.next = currentNode.next;
                    currentNode.next = newNode;
                    return pos;
                }
                currentNode = currentNode.next;
                pos++;
            }
            currentNode.next = newNode;
            return pos;
        }

    }

    public int isExist(double value) {
        for (int i = 0; i < getSize(); i++) {
            if (getValueByPosition(i) == value) return i;
        }
        return -1;
    }

    public void clearPoint() {
        Node currentNode = first;
        while (currentNode.next != null)
            currentNode = currentNode.next;
        first.next = currentNode;
        position = 0;
    }

    public double getValueByPosition(int pos) {
        Node currentNode = first;
        for (int i = 0; i < pos; i++) {
            if (currentNode == null) return -1;
            currentNode = currentNode.next;
        }
        if (currentNode == null) return -1;
        else return currentNode.value;
    }

    public String getDataStringByPosition(int pos) {
        Node currentNode = first;
        for (int i = 0; i < pos; i++) {
            if (currentNode == null) return null;
            currentNode = currentNode.next;
        }
        if (currentNode == null) return null;
        else return currentNode.data;
    }

    public Node getNodeByPosisiton(int pos) {
        Node currentNode = first;
        for (int i = 0; i < pos; i++) {
            if (currentNode == null) return null;
            currentNode = currentNode.next;
        }
        if (currentNode == null) return null;
        else return currentNode;
    }

    public void deleteByPosition(int pos) {
        if (pos > 0 && pos < getSize() - 1) {
            Node currentNode = first;
            Node previousNode = null;
            for (int i = 0; i < pos; i++) {
                previousNode = currentNode;
                currentNode = currentNode.next;
            }
            previousNode.next = currentNode.next;
            position = pos - 1;
        }
    }

    public void deleteCurrentPoint() {
        deleteByPosition(position);
    }

    public double getCurrentPoint() {
        return getValueByPosition(position);
    }

    public String getCurrentDataString(){
        return getDataStringByPosition(position);
    }

    public double getNextPoint() {
        Node nextNode = getNodeByPosisiton(position).next;
        if (nextNode != null) return nextNode.value;
        else return getNodeByPosisiton(getSize()).value;
    }

    public int getSize() {
        Node current = first;
        int size = 0;
        while (current != null) {
            current = current.next;
            size++;
        }
        return size;
    }

}
