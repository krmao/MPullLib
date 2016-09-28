# MPullLib
### apply to all android views
```
based on https://github.com/wcy10586/OverscrollLayout
thanks for @wcy10586
```

### preview gif

![gif](pull-refresh.gif "Logo Title Text 1")

###  real preview in android studio

![Alt text](code-0.png  =720x480)
![Alt text](code-1.png  =720x480)
![Alt text](code-2.png  =720x480)

### xml
```
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#eeeeee"
    android:orientation="vertical">

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="#83B7D5"
        tools:ignore="NestedWeights">

        <ImageView
            android:id="@+id/imageViewTop"
            android:layout_width="match_parent"
            android:layout_height="100dp"
            android:layout_gravity="top"
            android:contentDescription="@null"
            android:scaleType="centerCrop"
            android:src="@drawable/chuyin"
            tools:ignore="HardcodedText" />

        <ImageView
            android:id="@+id/imageViewBottom"
            android:layout_width="match_parent"
            android:layout_height="100dp"
            android:layout_gravity="bottom"
            android:contentDescription="@null"
            android:scaleType="centerCrop"
            android:src="@drawable/chuyin2"
            tools:ignore="HardcodedText" />

        <com.mlibrary.widget.pull.MPullToRefreshLayout
            android:id="@+id/mPullToRefreshLayout"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:pullBackground="@color/orange"
            app:pullHeaderBackground="@color/red"
            app:pullHeaderTextColor="@color/blue"
            app:pullEnableTouchWhileRefreshingOrLoading="true"
            app:pullTextColor="#333">

            <com.mlibrary.widget.pull.MPullLayout
                android:id="@+id/childPullLayout"
                android:layout_width="match_parent"
                android:layout_height="match_parent">

                <android.support.v7.widget.RecyclerView
                    android:id="@+id/recyclerView"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:background="@color/white" />
            </com.mlibrary.widget.pull.MPullLayout>
        </com.mlibrary.widget.pull.MPullToRefreshLayout>

    </FrameLayout>

</LinearLayout>

```
### java

```
public class HomeActivity extends MBaseFragmentActivity {
    private String TAG = "HomeActivity";
    private MPullToRefreshLayout mPullLayout;
    private MPullLayout childPullLayout;
    private long mExitTime = 0;
    private MRecyclerViewAdapter<ItemEntity, MViewHolder> mRecyclerViewAdapter;
    private ImageView imageViewTop;
    private ImageView imageViewBottom;


    public static class ItemEntity {
        public String content;
        public String imageUrl;
        public int oldIndex;

        public ItemEntity(String content, String imageUrl, int oldIndex) {
            this.content = content;
            this.imageUrl = imageUrl;
            this.oldIndex = oldIndex;
        }
    }

    private int topImageViewHeight = 0;
    private int bottomImageViewHeight = 0;
    private int COLOR_DEFAULT = Color.parseColor("#2AA5F8");
    private int COLOR_SELECTED = Color.parseColor("#AAA5F8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MPullToRefreshLayout.setGlobalDurationFooterToLoading(3000);
        MPullToRefreshLayout.setGlobalDurationFooterToNormal(3000);
        MPullToRefreshLayout.setGlobalDurationHeaderToNormal(2000);
        MPullToRefreshLayout.setGlobalDurationHeaderToRefreshing(2000);

        mPullLayout = (MPullToRefreshLayout) findViewById(R.id.mPullToRefreshLayout);
        childPullLayout = (MPullLayout) findViewById(R.id.childPullLayout);
        imageViewTop = (ImageView) findViewById(R.id.imageViewTop);
        imageViewBottom = (ImageView) findViewById(R.id.imageViewBottom);

        List<ItemEntity> initDataList = new ArrayList<>();
        for (int i = 0; i < 20; i++)
            initDataList.add(new ItemEntity(null, MTestUtil.getRandomTransparentAvatar(), i));

        mRecyclerViewAdapter = new MRecyclerViewAdapter<ItemEntity, MViewHolder>(this, initDataList) {
            @Override
            public MViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MViewHolder(LayoutInflater.from(context).inflate(R.layout.image_layout, parent, false));
            }

            @Override
            public void onBindViewHolder(final MViewHolder holder, int position) {
                ItemEntity itemEntity = mRecyclerViewAdapter.getDataList().get(position);
                MFrescoUtil.showProgressiveImage(itemEntity.imageUrl, holder.simpleDraweeView);
                holder.text.setText(String.format(Locale.getDefault(), "oldIndex:%d currentPosition:%d", itemEntity.oldIndex, position));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MToastUtil.show(holder.text.getText().toString().trim());
                    }
                });
            }
        };

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mRecyclerViewAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        final int model = childPullLayout.getOverScrollMode();
        //增加滑动处理,需 viewHolder 继承 ItemTouchHelperViewHolder
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(mRecyclerViewAdapter, new OnDragListener() {
            @Override
            public void onDragBegin(RecyclerView.ViewHolder viewHolder, int actionState) {
                childPullLayout.setOverScrollMode(MPullLayout.MODE_NONE);
                MViewHolder mViewHolder = (MViewHolder) viewHolder;
                mViewHolder.cardView.setCardBackgroundColor(COLOR_SELECTED);
            }

            @Override
            public void onDragEnd(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                childPullLayout.setOverScrollMode(model);
                MViewHolder mViewHolder = (MViewHolder) viewHolder;
                mViewHolder.cardView.setCardBackgroundColor(COLOR_DEFAULT);
            }
        }));
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mPullLayout.setDebugAble(true);//debug
        mPullLayout.setOnOverScrollListener(new OnOverScrollListener() {
            @Override
            public void onOverScroll(int currentX, int currentY, boolean isInDrag) {
                Log.d(TAG, "currentY:" + currentY);
                if (topImageViewHeight == 0) {
                    topImageViewHeight = imageViewTop.getMeasuredHeight();
                }
                if (bottomImageViewHeight == 0) {
                    bottomImageViewHeight = imageViewBottom.getMeasuredHeight();
                }
                ViewGroup.LayoutParams topLayoutParams = imageViewTop.getLayoutParams();
                ViewGroup.LayoutParams bottomLayoutParams = imageViewBottom.getLayoutParams();

                int absY = Math.abs(currentY);

                if (currentY < 0) {
                    topLayoutParams.height = absY > topImageViewHeight ? absY : topImageViewHeight;
                    imageViewTop.setLayoutParams(topLayoutParams);
                } else {
                    bottomLayoutParams.height = absY > bottomImageViewHeight ? absY : bottomImageViewHeight;
                    imageViewBottom.setLayoutParams(bottomLayoutParams);
                }
            }
        });
        mPullLayout.setOnPullRefreshListener(new OnPullRefreshListener() {
            @Override
            public void onRefresh() {
                currentPage = 0;
                doAsyncRequest(true);
            }

            @Override
            public void onLoadMore() {
                doAsyncRequest(false);
            }
        });
    }

    private int currentPage = 0;
    private static int totalPage = 8;

    public void doAsyncRequest(final boolean isRefreshNotLoading) {
        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        currentPage = currentPage + 1;//设置当前currentPage
                        if (isRefreshNotLoading) {//下拉刷新,清空原有数据
                            mRecyclerViewAdapter.removeAll();
                            mPullLayout.completeHeaderRefreshSuccess();
                        } else {//加载更多
                            if (currentPage >= totalPage) {
                                if (mPullLayout != null)
                                    mPullLayout.completeFooterLoadNoMoreData();
                            } else {
                                if (mPullLayout != null)
                                    mPullLayout.completeFooterLoadSuccess();
                            }
                        }
                        List<ItemEntity> tmpDataList = new ArrayList<>();
                        for (int i = 0; i < 20; i++)
                            tmpDataList.add(new ItemEntity(null, MTestUtil.getRandomTransparentAvatar(), mRecyclerViewAdapter.getDataList().size() + i));
                        mRecyclerViewAdapter.add(tmpDataList);

                        if (mRecyclerViewAdapter.getDataList().isEmpty())
                            mPullLayout.completeFooterLoadNoMoreData();
                        else if (currentPage < totalPage)
                            mPullLayout.resetFooterView();

                    }
                }, 3000);
    }

    public static class MViewHolder extends RecyclerView.ViewHolder {
        public SimpleDraweeView simpleDraweeView;
        public TextView text;
        public CardView cardView;

        public MViewHolder(View view) {
            super(view);
            simpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.simpleDraweeView);
            text = (TextView) view.findViewById(R.id.text);
            cardView = (CardView) view.findViewById(R.id.cardView);
        }
    }
}

```

