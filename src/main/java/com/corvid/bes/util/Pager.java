package com.corvid.bes.util;

import java.io.Serializable;

public class Pager implements Serializable {

    public static final String TOTAL_COUNT = "X-Total-Count";

    public static final String CURRENT_PAGE = "X-Current-Page";

    public static final String START = "X-Record-start";

    public static final String END = "X-Record-End";

    private int pageSize = 25;

    private int page = 1;

    private int numOfRecords = 0;

    public Pager() {
    }

    public Pager(int pageSize) {
        this.pageSize = pageSize;
    }


    public Pager(int pageSize, int page, int numOfRecords) {
        this.pageSize = pageSize;
        //this.page = page;
        setPage(page);
        this.numOfRecords = numOfRecords;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        if (page <= 0) {// avoid negative pages
            page = getFirstPage();
        }
        this.page = page;
    }

    public int getNumOfRecords() {
        return numOfRecords;
    }

    public void setNumOfRecords(int numOfRecords) {
        this.numOfRecords = numOfRecords;
    }

    public int getNextPage() {
        return page + 1;
    }

    public int getPreviousPage() {
        return page - 1;
    }

    public int getFirstPage() {
        return 1;
    }

    public long getLastPage() {
        long lastPage = (numOfRecords / pageSize);
        if (numOfRecords % pageSize == 0) lastPage--;
        return lastPage + 1;
    }

    public int getIndexBegin() {
        return (getPage() - 1) * getPageSize();
    }

    public int getIndexEnd() {
        int firstIndex = getIndexBegin();
        int pageIndex = getPageSize() - 1;
        int lastIndex = getNumOfRecords() - 1;
        final int min = Math.min(firstIndex + pageIndex, lastIndex);
        return min > 0 ? min : 0;
    }

    public boolean isPreviousPageAvailable() {
        return getIndexBegin() + 1 > getPageSize();
    }

    public boolean isNextPageAvailable() {
        return numOfRecords - 1 > getIndexEnd();
    }

    public boolean isSeveralPages() {
        return getNumOfRecords() != 0 && getNumOfRecords() > getPageSize();
    }

    public int getFirstRecord() {
        return (pageSize * page) - pageSize + 1;
        //return page * pageSize + 1;
    }

    public int getLastRecord() { //
        return (page * pageSize + pageSize) > numOfRecords
                ? numOfRecords
                : page * pageSize + pageSize;
    }

    public String toString() {
        return "Pager - Records: " + getNumOfRecords() + ", Page size: " + getPageSize() + ",Current Page: "
                + getPage() + ", Index Range Begin: " + getIndexBegin() + ", Index Range End  " + getIndexEnd() +
                " Last Record: " + getLastRecord() + " ";
    }

    //Some quick and dirty tests
    public static void main(String[] args) {
        Pager pager = new Pager(10, 1, 30);

        assert pager.getNumOfRecords() == 30;

        assert pager.getPage() == 1;
        assert pager.getFirstRecord() == 1;
        assert pager.getLastRecord() == 10;
        assert pager.isNextPageAvailable();
        assert !pager.isPreviousPageAvailable();
        assert pager.getNextPage() == 2;
        assert pager.getIndexBegin() == 0;
        assert pager.getIndexEnd() == 9;

        pager.setPage(pager.getNextPage());
        assert !pager.isNextPageAvailable();
        assert pager.isPreviousPageAvailable();
        assert pager.getPreviousPage() == 1;
        assert pager.getIndexBegin() == 3;
        assert pager.getIndexEnd() == 19;
    }
}
