package com.wj.yuanbin.rollbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.List;

/**
 * Created by yuanbin on 16/8/2.
 */
public class RollLayout extends ViewGroup{

    /***
     * rollout 模式:
     *
     * direction---topToBottom mRollView 放在mSurfaceView的上面
     *
     * direction---bottomToTop mRollView 放在mSurfaceView的下面
     *
     * direction---leftToRight mRollView 放在mSurfaceView的左边
     *
     * direction---rightToLeft mRollView 放在mSurfaceView的右边
     *
     */
    public static final int ROLLOUT = 0;
    /***
     * laydown 模式:
     *
     * direction---topToBottom,bottomToTop,leftToRight,rightToLeft
     *
     * mRollView 放在mSurfaceView遮挡mRollView
     */
    public static final int LAYDOWN = 1;


    /***
     * 滚动方向 top -> bottom
     */
    public static final int TOPTOBOTTOM = 0;

    /**
     * 滚动方向 bottom -> top
     */
    public static final int BOTTOMTOTOP = 1;
    /***
     * 滚动方向 left -> right
     */
    public static final int LEFTTORIGHT = 2;

    /***
     * 滚动方向 right -> left
     */
    public static final int RIGHTTOLEFT = 3;

    /***
     * 滚动开始
     */
    public static final int START = 0;
    /**
     * 滚动中 start -> pause -> rolling -> pause ....... -> end
     */
    public static final int ROLLING = 1;
    /***
     * 滚动间隙暂停
     */
    public static final int PAUSE = 2;
    /***
     * 滚动停止，保持滚动状态
     */
    public static final int STOP = 3;
    /***
     * 回复状态
     */
    public static final int RESUME = 5;
    /***
     * 滚动结束
     */
    public static final int END = 4;

    private int[] mStates = new int[]{START,ROLLING,PAUSE,STOP,RESUME,END};
    /***
     * RollLayout 的布局模式
     * 主要分为ROLLOUT 和 LAYDOWN两种
     */
    private int mMode = ROLLOUT;

    /***
     * 滚动方向 默认TOPTOBOTTOM
     *
     * 可选TOPTOBOTTOM,BOTTOMTOTOP,LEFTTORIGHT,RIGHTTOLEFT
     *
     */
    private int mRollDirection = TOPTOBOTTOM;

    /****
     * 滚动状态：
     *
     * 包含5个状态：START,ROLLING,PAUSE,STOP,END
     */
    private int mState = -1;

    private Object mLock = new Object();

    /***
     *
     * mSurfaceView
     *
     * mRollView 滚动View
     */
    private View mSurfaceView,mRollView;

    /***
     * mSurfaceView 和 mRollView 的MarginLayoutParams
     */
    private MarginLayoutParams mMarginLayoutParams;

    private int childWidth,childHeight;

    /***
     * 中途停顿时间,默认300毫秒
     */
    private int mPauseTime = 300;

    /***
     * 滚动持续时间,默认500毫秒
     */
    private int mDuration = 1000;

    /***
     * 循环次数，默认1次
     */
    private int mCirculationTimes = 1;

    /***
     * 当前位置
     */
    private int mCurrentPosition = 0;

    /***
     * 实际循环的次数
     */
    private int mTotalCirculationTimes = 0;

    /***
     * 插值
     */
    private Interpolator interpolator = new LinearInterpolator();

    /**每一步25毫秒**/
    private int mStepTime = 25;

    /***
     * rollAdapter
     */
    private RollAdapter mRollAdapter;


    public RollLayout(Context context) {
        super(context);
    }

    public RollLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public RollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, -1);
    }

    public RollLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    /***
     * 设置模式
     * @param mode
     */
    public void setMode(int mode){
        mMode = mode;
    }

    /***
     * 设置方向
     * @param direction
     */
    public void setDirection(int direction){
        mRollDirection = direction;
        setState(END);
        setState(START);
    }

    /**
     * setPauseTime
     * @param mPauseTime
     */
    public void setPauseTime(int mPauseTime) {
        this.mPauseTime = mPauseTime;
    }

    /***
     * setDuration
     * @param mDuration
     */
    public void setDuration(int mDuration) {
        this.mDuration = mDuration;
    }

    /***
     * setCirculationTimes
     * @param mCirculationTimes
     */
    public void setCirculationTimes(int mCirculationTimes) {
        this.mCirculationTimes = mCirculationTimes;
    }

    /**
     * setCurrentPosition
     * @param mCurrentPosition
     */
    public void setCurrentPosition(int mCurrentPosition) {
        this.mCurrentPosition = mCurrentPosition;
        resetSurfaceViewAndRollView();
    }

    /***
     * init
     * @param context
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        TypedArray typedArray = context.obtainStyledAttributes(attrs,R.styleable.RollLayout,defStyleAttr,defStyleRes);

        mMode = typedArray.getInteger(R.styleable.RollLayout_mode,mMode);
        mRollDirection = typedArray.getInteger(R.styleable.RollLayout_direction,mRollDirection);
        mPauseTime = typedArray.getInteger(R.styleable.RollLayout_pauseTime,mPauseTime);
        mDuration = typedArray.getInteger(R.styleable.RollLayout_duration,mDuration);
        mCirculationTimes = typedArray.getInteger(R.styleable.RollLayout_circulation,mCirculationTimes);
        int viewLayoutId = typedArray.getResourceId(R.styleable.RollLayout_layoutId,-1);
        int interpolatorId = typedArray.getResourceId(R.styleable.RollLayout_interpolator,-1);
        typedArray.recycle();
        if(mCirculationTimes == -1) mCirculationTimes = Integer.MAX_VALUE;
        if (mCirculationTimes == 0) mCirculationTimes = 1;
        if (interpolatorId != -1)interpolator = AnimationUtils.loadInterpolator(context,interpolatorId);
        if (viewLayoutId == -1){
            throw new IllegalArgumentException("layoutId is -1!");
        }
        if (getChildCount() > 0){
            throw new IllegalArgumentException("RollLayout child(children) can't add in xml!");
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        /***填充两个子View,第一个是mRollView,第二个是mSurfaceView***/
        inflater.inflate(viewLayoutId,this);
        inflater.inflate(viewLayoutId,this);
        mRollView = getChildAt(0);mSurfaceView=getChildAt(1);
        mMarginLayoutParams = (MarginLayoutParams)mSurfaceView.getLayoutParams();

    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(),attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p != null && p instanceof MarginLayoutParams;
    }

    /****
     * 重写onLayout
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mMode == LAYDOWN){
            layoutLayDown(changed,l,t,r,b);
        }else if (mMode == ROLLOUT){
            layoutRollOut(changed,l,t,r,b);
        }
    }

    /***
     *
     * 获取mSurfaceView rect
     * @return
     */
    private Rect getSurfaceViewRect(int l, int t, int r, int b){

        int width = r - l;int height = b - t;

        int left = getPaddingLeft()+mMarginLayoutParams.leftMargin;

        int top = getPaddingTop()+mMarginLayoutParams.topMargin;

        int right = width-getPaddingRight()-mMarginLayoutParams.rightMargin;

        int bottom = height-getPaddingBottom()-mMarginLayoutParams.bottomMargin;

        if (childWidth !=0 || childHeight != 0) {
            int right1 = left + childWidth;

            int bottom1 = top + childHeight;

            if (right1 < right) right = right1;

            if (bottom1 < bottom) bottom = bottom1;
        }
        return new Rect(left,top,right,bottom);
    }
    /***
     * 当mMode 为LAYDOWN
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    private void layoutLayDown(boolean changed, int l, int t, int r, int b){
        Rect rect = getSurfaceViewRect(l,t,r,b);
        mSurfaceView.layout(rect.left,rect.top,rect.right,rect.bottom);
        mRollView.layout(rect.left, rect.top, rect.right, rect.bottom);
    }

    /***
     * 当mMode 为ROLLOUT
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    private void layoutRollOut(boolean changed, int l, int t, int r, int b){
        /****布局mSurfaceView*****/
        Rect rect = getSurfaceViewRect(l,t,r,b);
        mSurfaceView.layout(rect.left, rect.top, rect.right, rect.bottom);
        /****布局mRollView***/
        switch (mRollDirection){
            case TOPTOBOTTOM :
                mRollView.layout(rect.left,rect.top-childHeight-mMarginLayoutParams.bottomMargin-mMarginLayoutParams.topMargin,
                        rect.right,rect.bottom-mMarginLayoutParams.bottomMargin-mMarginLayoutParams.topMargin);
                break;

            case BOTTOMTOTOP :
                mRollView.layout(rect.left,rect.top+childHeight+mMarginLayoutParams.bottomMargin+mMarginLayoutParams.topMargin,
                        rect.right,rect.bottom+childHeight+mMarginLayoutParams.bottomMargin+mMarginLayoutParams.topMargin);
                break;

            case LEFTTORIGHT :
                mRollView.layout(rect.left-childWidth-mMarginLayoutParams.leftMargin-mMarginLayoutParams.rightMargin,rect.top,
                        rect.right-childWidth-mMarginLayoutParams.leftMargin-mMarginLayoutParams.rightMargin,rect.bottom);
                break;

            case RIGHTTOLEFT :
                mRollView.layout(rect.left+childWidth+mMarginLayoutParams.leftMargin+mMarginLayoutParams.rightMargin,rect.top,
                        rect.right+childWidth+mMarginLayoutParams.leftMargin+mMarginLayoutParams.rightMargin,rect.bottom);
                break;
        }
    }

    /***
     * 重写onMeasure
     *
     * mSurfaceView 和 mRollView 尺寸一样占据整个RollView控件
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        measureChildWithMargins(mSurfaceView, widthMeasureSpec, 0, heightMeasureSpec, 0);

        measureChildWithMargins(mRollView, widthMeasureSpec, 0, heightMeasureSpec, 0);

        int leftAndRightSpace = getPaddingLeft()+getPaddingRight() + mMarginLayoutParams.leftMargin+mMarginLayoutParams.rightMargin;
        int topAndBottomSpace = getPaddingBottom()+getPaddingTop()+mMarginLayoutParams.topMargin+mMarginLayoutParams.bottomMargin;

        childWidth = mSurfaceView.getMeasuredWidth();childHeight = mSurfaceView.getMeasuredHeight();
        setMeasuredDimension(resolveSizeAndState(mRollView.getMeasuredWidth()+leftAndRightSpace, widthMeasureSpec, mRollView.getMeasuredState()),
                resolveSizeAndState(mRollView.getMeasuredHeight()+topAndBottomSpace, heightMeasureSpec, mRollView.getMeasuredState()));
    }

    /***
     * setAdapter
     * @param rollAdapter
     */
    public void setAdapter(RollAdapter rollAdapter){
        if (rollAdapter == null || rollAdapter.getCount() == 0)return;
        mRollAdapter = rollAdapter;
        mCurrentPosition = 0;
        mRollAdapter.getView(mCurrentPosition, mSurfaceView);
        if (rollAdapter.getCount()>1)
        mRollAdapter.getView(mCurrentPosition+1,mRollView);
    }

    /***
     * start
     */
    public void start(){
        setState(START);
    }

    /**
     * stop
     */
    public void stop(){
        setState(STOP);
    }

    /**
     * end
     */
    public void end(){
        setState(END);
    }

    /**
     * resume
     */
    public void resume(){
        setState(RESUME);
    }

    /***
     * 设置当前滚动状态
     * @param state
     */
    private void setState(int state){
        synchronized (mLock) {
            if (mState != state) {
                //mState = state;
                Message message = getMessage(state);
                rollHandler.sendMessage(message);
            }
        }
    }

    /****
     * 根据当前状态获取Message
     * @param state
     * @return
     */
    private Message getMessage(int state){
        Message message = Message.obtain();
        message.what = state;
        return message;
    }

    /***
     * 重新设置
     */
    private void resetSurfaceViewAndRollView(){
        mRollAdapter.getView(mCurrentPosition,mSurfaceView);
        mRollAdapter.getView((mCurrentPosition+1)%mRollAdapter.getCount(),mRollView);
        requestLayout();
    }


    private void moveChild(int move,int method){
        switch (mMode){
            case LAYDOWN:
                if (method == 0){
                    mSurfaceView.offsetTopAndBottom(move);
                }else if (method == 1){
                    mSurfaceView.offsetLeftAndRight(move);
                }
                break;
            case ROLLOUT:
                if (method == 0){
                    mSurfaceView.offsetTopAndBottom(move);
                    mRollView.offsetTopAndBottom(move);
                }else if (method == 1){
                    mSurfaceView.offsetLeftAndRight(move);
                    mRollView.offsetLeftAndRight(move);
                }
                break;
        }
    }
    /***
     * 处理滚动handler
     */
    private Handler rollHandler = new Handler(){

        private int consumeTime;
        private float restMove;
        private float floatMove;
        private int beforeStopState;
        private int totalMove;

        private boolean stop;
        private void reset(){
            consumeTime = 0;
            restMove = 0;
            floatMove = 0;
            totalMove = 0;
        }

        private void roll(){
            if (stop) return;
            float g = interpolator.getInterpolation((float)consumeTime/mDuration);
            float g1 = interpolator.getInterpolation((float)(consumeTime+mStepTime)/mDuration);
            int move = 0;
            int callMethod = 0;
            int moveTo;
            boolean rollPause = false;
            switch (mRollDirection){
                case TOPTOBOTTOM:
                    callMethod = 0;
                    floatMove = mSurfaceView.getMeasuredHeight()*(g1-g)+restMove;
                    move = (int)floatMove;
                    restMove = floatMove - move;
                    moveTo = childHeight+mMarginLayoutParams.topMargin+mMarginLayoutParams.bottomMargin;
                    if (totalMove+move >= moveTo){
                        move = moveTo - totalMove;
                        restMove = 0;
                        rollPause = true;
                    }
                    break;
                case BOTTOMTOTOP:
                    callMethod = 0;
                    floatMove = -mSurfaceView.getMeasuredHeight()*(g1-g)+restMove;
                    move = (int)floatMove;
                    restMove = floatMove - move;
                    moveTo = -childHeight-mMarginLayoutParams.topMargin-mMarginLayoutParams.bottomMargin;
                    if (totalMove+ move <= moveTo){
                        move = moveTo - totalMove;
                        restMove = 0;
                        rollPause = true;
                    }
                    break;

                case LEFTTORIGHT:
                    callMethod = 1;
                    floatMove = mSurfaceView.getMeasuredWidth()*(g1-g)+restMove;
                    move = (int)floatMove;
                    restMove = floatMove - move;
                    moveTo = childWidth+mMarginLayoutParams.leftMargin+mMarginLayoutParams.rightMargin;
                    if (totalMove+move >= moveTo){
                        move = moveTo - totalMove;
                        restMove = 0;
                        rollPause = true;
                    }
                    break;
                case RIGHTTOLEFT:
                    callMethod = 1;
                    floatMove = -mSurfaceView.getMeasuredWidth()*(g1-g)+restMove;
                    move = (int)floatMove;
                    restMove = floatMove - move;
                    moveTo = -childWidth-mMarginLayoutParams.leftMargin - mMarginLayoutParams.rightMargin;
                    if (totalMove+move <= moveTo){
                        move = moveTo - totalMove;
                        restMove = 0;
                        rollPause = true;
                    }
                    break;
            }
            moveChild(move, callMethod);
            if (rollPause){
                rollHandler.sendMessage(getMessage(PAUSE));
            }else {
                rollHandler.sendMessageDelayed(getMessage(ROLLING), mStepTime);
                consumeTime+=mStepTime;
                totalMove+=move;
            }
        }


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                /**开始滚动**/
                case START:
                    mState = START;
                    resetSurfaceViewAndRollView();
                    removeOtherStateMessages(START);
                    rollHandler.sendMessageDelayed(getMessage(ROLLING),mPauseTime);
                    break;
                /***滚动中**/
                case ROLLING:
                    mState = ROLLING;
                    roll();
                    break;
                /***间断***/
                case PAUSE:
                    reset();
                    mState = PAUSE;
                    removeOtherStateMessages(PAUSE);
                    if (mCurrentPosition+1 >= mRollAdapter.getCount()-1){
                        mTotalCirculationTimes ++;
                    }
                    mCurrentPosition++;
                    mCurrentPosition %= mRollAdapter.getCount();
                    if (mTotalCirculationTimes < mCirculationTimes){
                        resetSurfaceViewAndRollView();
                        rollHandler.sendMessageDelayed(getMessage(ROLLING),mPauseTime);
                    }else {
                        rollHandler.sendMessage(getMessage(END));
                    }
                    break;
                /**暂停**/
                case STOP:
                    removeOtherStateMessages(STOP);
                    /***保存当前状态***/
                    beforeStopState = mState;
                    mState = STOP;
                    stop = true;
                    break;
                case RESUME:
                    rollHandler.sendMessage(getMessage(beforeStopState));
                    stop = false;
                    break;
                /***结束**/
                case END:
                    reset();
                    mState = END;
                    removeOtherStateMessages(END);
                    resetSurfaceViewAndRollView();
                    break;
            }
        }
    };

    /***
     * 删除message
     * @param state
     */
    private void removeOtherStateMessages(int state){
        for (int item : mStates){
            if (state == item)continue;
            rollHandler.removeMessages(state);
        }
    }
    /***
     * Roll数据Adapter
     */
    public static abstract class RollAdapter<T>{

        List<T> datas;

        public RollAdapter(List<T> datas){
            this.datas = datas;
        }


        public int getCount(){
            return datas == null ? 0 : datas.size();
        }

        public T getItem(int position){
            return datas.get(position);
        }

        protected void getView(int position,View view){
            if (view == null) throw new NullPointerException("一般不会为空！");
            ViewHolder tag = (ViewHolder)view.getTag();
            if (tag == null){
                tag = new ViewHolder(view);
                view.setTag(tag);
            }
            refreshView(position,tag);
        }

        public abstract void refreshView(int position, ViewHolder viewHolder);

    }


    public static class ViewHolder{

        SparseArray<View> sparseArray = null;

        Context context;

        View rootView;

        public ViewHolder(View view){
            if (view == null) throw new NullPointerException(" view is null!");
            rootView = view;
            context = view.getContext();
        }

        /***
         * getView
         * @param resId
         * @param <T>
         * @return
         */
        public <T extends View> T getView(int resId){
            if (sparseArray == null) sparseArray = new SparseArray<View>();
            View view = sparseArray.get(resId);
            if (view == null){
                view = rootView.findViewById(resId);
                if (view != null)sparseArray.put(resId,view);
                else throw new IllegalArgumentException("resId is available!");
            }
            return (T)view;
        }
    }

}
