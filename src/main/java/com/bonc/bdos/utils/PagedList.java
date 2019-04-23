package com.bonc.bdos.utils;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果集的封装.
 * 
 * @author anychem
 */
public class PagedList<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static int defaultPage = 0;
    private static int defaultPageSize = 10;

    /**
     * @since anychem @ 2010-5-18
     */

    /**
     * 当前页记录列表.
     */
    private List<T> pageItems;
    /**
     * 总记录列表.
     */
    private List<T> items;

    /**
     * 当前页码, 0表示第一页.
     */
    private int pageIndex;

    /**
     * 每页记录数.
     */
    private int pageSize;

    /**
     * 总记录数.
     */
    private int totalItemCount;

    /**
     * 当前页的总记录数.
     */
    private int thisPageTotal;

    /**
     * 总页数.
     */
    private int pageTotal;

    /**
     * 当前页的上一页.
     */
    private int prevPage;

    /**
     * 当前页的下一页.
     */
    private int nextPage;

    /**
     * 
     * @param pageItems
     * @param totalItemCount
     * @since anychem @ Jul 6, 2010
     */
    public PagedList(List<T> items) {
        this(items, defaultPage, defaultPageSize);
    }

    /**
     * 构造分页结果集. 其中将{@link #step}设置为10.
     * 
     * @see #PagedList(int, int, int, List, int)
     */
    public PagedList(List<T> items, int pageIndex, int pageSize) {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.totalItemCount = items.size();
        this.items = items;

        computePageIndex();
    }

    /**
     * 计算当前页的数据list及分页相关.
     * 
     * @param stepValue
     *            页码导航显示多少页.
     */
    private void computePageIndex() {
        if (totalItemCount <= 0) {
            pageTotal = 0;
        } else {
            pageTotal = (totalItemCount / pageSize) + ((totalItemCount % pageSize == 0) ? 0 : 1);
        }
        prevPage = (pageIndex == 0) ? 0 : pageIndex - 1;
        nextPage = (pageIndex >= pageTotal - 1) ? pageTotal - 1 : pageIndex + 1;

        int startRow = this.pageIndex * this.pageSize;
        int endRow = (this.pageIndex + 1) * this.pageSize;
        if (endRow >= totalItemCount) {

            endRow = totalItemCount;
        }

        pageItems = items.subList(startRow, endRow);
    }

    /**
     * 返回当前页的第index条记录.
     */
    public T get(int index) {
        return pageItems.get(index);
    }

    /**
     * @return the list of items for this page
     */
    public List<T> getPageItems() {
        return pageItems;
    }

    /**
     * @return total count of items
     */
    public int getTotalItemCount() {
        return totalItemCount;
    }

    /**
     * @return total count of pages
     */
    public int getTotalPageCount() {
        return getPageTotal();
    }

    /**
     * @return Returns the pageTotal.
     */
    public int getPageTotal() {
        return pageTotal;
    }

    /**
     * 返回第一页(首页)的页码.
     */
    public int getFirstPageNo() {
        return 0;
    }

    /**
     * 返回最后一页(末页)的页码.
     */
    public int getLastPageNo() {
        return pageTotal - 1;
    }

    /**
     * @return true if this is the first page
     */
    public boolean isFirstPage() {
        return isFirstPage(getPageIndex());
    }

    /**
     * @return true if this is the last page
     */
    public boolean isLastPage() {
        return isLastPage(getPageIndex());
    }

    /**
     * @param page
     * @return true if the page is the first page
     */
    public boolean isFirstPage(int page) {
        return page <= 0;
    }

    /**
     * @param page
     * @return true if the page is the last page
     */
    public boolean isLastPage(int page) {
        return page >= getTotalPageCount() - 1;
    }

    /**
     * @return the pageIndex
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * @return the pageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * @return the thisPageTotal.
     */
    public int getThisPageTotal() {
        return thisPageTotal;
    }

    /**
     * @return prevPage
     */
    public int getPrevPage() {
        return prevPage;
    }

    /**
     * @return nextPage
     */
    public int getNextPage() {
        return nextPage;
    }

    /**
     * @see java.lang.Object#toString()
     * @creator anychem @ Jan 27, 2010
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PagedList [pageIndex=").append(pageIndex);
        builder.append(", total=").append(totalItemCount);
        builder.append(", thisPageTotal=").append(thisPageTotal);
        if (pageItems != null) {
            builder.append("; pageItems=").append(pageItems);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 获取当前结果集的记录数
     * 
     * @return
     * @since anychem @ Nov 14, 2010
     */
    public int size() {
        return (pageItems == null) ? 0 : pageItems.size();
    }

    /**
     * 获取当前结果集的开始记录数
     * 
     * @since anychem @ 2013-8-24
     */
    protected int getStartIndex() {
        return ((pageIndex > 1 ? pageIndex : 1) - 1) * this.pageSize + 1;
    }

    /**
     * 更新结果集，供子类使用
     * 
     * @since anychem @ 2013-8-24
     */
    protected void setPageItems(List<T> pageItems) {
        this.pageItems = pageItems;
    }
}
