package com.abysmel.dashspinner;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by Melvin Lobo on 2/5/2016.
 * The behaviour of the Dash Spinner is as follows:
 *
 * 1. DOWNLOAD and SUCCESS:
 * 		The progress is set externally using {@link DashSpinner#setProgress}. It will perform the following animations:
 * 		a. The Inner circle will grow as per the download percentage
 * 		b. A text in the center will move from an initial value to a final value as it scales (if it is showing)
 * 		After the download is done, {@link DashSpinner#showSuccess} is called to show success which will perform three animations
 * 	    a. Scale down the text in the center (if it is showing) to zero and convert it to a dot
 * 	    b. Animate the dot to a horizontal line
 * 	    c. Animate the line to a tick mark
 *
 * 2. DOWNLOAD and FAILURE / UNKNOWN:
 * 		The progress is set externally using {@link DashSpinner#setProgress}. If the download fails / throws an unknown error for
 * 	    some reason, {@link DashSpinner#showFailure()} / {@link DashSpinner#showUnknown()} is called. This will perform the following animations :
 * 	    a. Scale down the progress text (if it is showing) to zero, converting it to a dot and complete the growing inner circle for download
 * 	       but with the color set for failure / unknown (the alpha will continue to reach to its maximum value of 255 with the new failure / unknown color)
 * 	    b. Animate the dot to a horizontal line
 * 	    c. Animate the line to a cross mark / exclamation
 *
 * 3. FAILURE / UNKNOWN
 *     {@link DashSpinner#showFailure()} / {@link DashSpinner#showUnknown()} is called externally. This will perform the following animations:
 *     a. Animate the inner circle with the failure / unknown color and alpha (0 to 255) and scale the text (0%) down and convert it to a dot
 *     b. Animate the dot to a horizontal line
 * 	   c. Animate the line to a cross mark / exclamation
 */
public class DashSpinner extends View {

	//////////////////////////////////////// CLASS MEMBERS /////////////////////////////////////////
	/**
	 * Static values
	 */
	private static final float DEFAULT_START_SPEED           = 20.0f;
	private static final float DEFAULT_ARC_WIDTH             = 6.0f;
	private static final float DEFAULT_RING_WIDTH            = 2.0f;
	private static final int   CIRCULAR_FACTOR               = 360;
	private static final float ARC_START_POSITION            = 270.0f;
	private static final float DEFAULT_ARC_LENGTH            = 90.0f;
	private static final int   DEFAULT_MAX_TEXT_SIZE         = 40;
	private static final int   TEXT_PADDING                  = 8;
	private static final int   TRANSITION_ANIM_DURATION      = 400;
	private static final float TRANSITION_CAT_START_VAL      = 1.0f;
	private static final float TRANSITION_CAT_END_VAL        = 0.0f;
	private static final float STATUS_SYMBOL_WIDTH_PERCENT   = 0.5f;    //Take 50% of the available width to draw the status symbols
	private static final float TEXT_SCALE_DOWN_PERCENT_VALUE = 0.1f;
	private static final float STATE_LINE_STROKE             = 4.0f;
	private static final float TICK_SHORT_ARM_RATIO_PERCENT  = 0.25f;
	private static final float TICK_LONG_ARM_RATIO_PERCENT   = 0.75f;
	private static final float ARM_ANGLE                     = 45.0f;
	private static final float TICK_SHORT_ARM_ANGLE_RADIANS  = ARM_ANGLE;
	private static final float TICK_LONG_ARM_ANGLE_RADIANS   = -ARM_ANGLE;
	private static final float UNKNOWN_DOT_DISTANCE          = 10.0f;        //Final distance of the dot from the line forming an exclamation (!)
	private static final float UNKNOWN_ROTATION_ANGLE        = 90.0f;
	private static final int   MAX_ALPHA                     = 255;


	/**
	 * Enum to define the Modes that the Dash Spinner will go through
	 */
	public enum DASH_MODE {
		NONE, DOWNLOAD, TRANSITION_TEXT_AND_CIRCLE, TRANSITION_LINE, SUCCESS, FAILURE, UNKNOWN
	}

	/**
	 * The current mode that the Dash Spinner is in
	 */
	private DASH_MODE mCurrentDashMode = DASH_MODE.NONE;

	/**
	 * The Next Mode that should come after transiton
	 */
	private DASH_MODE mNextDashMode = DASH_MODE.NONE;

	/**
	 * The progress Text
	 */
	private String msProgressText = "";

	/**
	 * The Outer ring color
	 */
	private int mOuterRingColor = 0;

	/**
	 * The Arc color
	 */
	private int mArcColor = 0;

	/**
	 * The Inner Circle Download / Success color
	 */
	private int mInnerCircleSuccessColor = 0;

	/**
	 * The Inner Circle Failure color
	 */
	private int mInnerCircleFailureColor = 0;

	/**
	 * The Inner Circle Unknown color
	 */
	private int mInnerCircleUnknownColor = 0;

	/**
	 * The Text Color From
	 */
	private int mTextColorFrom = 0;

	/**
	 * The Text Color To
	 */
	private int mTextColorTo = 0;

	/**
	 * The Max text size of the text indicating the percentage value
	 */
	private int mnMaxTextSize = DEFAULT_MAX_TEXT_SIZE;

	/**
	 * The paint used to draw
	 */
	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	/**
	 * The paint for the text
	 */
	private TextPaint mTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);

	/**
	 * The Arc width
	 */
	private float mnArcWidth = 0.0f;

	/**
	 * The Ring Width
	 */
	private float mnRingWidth = 0.0f;

	/**
	 * The arc rect
	 */
	private final RectF mArcRect = new RectF();

	/**
	 * The progress variable
	 */
	private float mnIndeterminateStartPosition = 0;

	/**
	 * The current Speed factor
	 */
	private float mnStartSpeed = 0.0f;

	/**
	 * The progress factor between 0 and 1
	 */
	private float mnProgress = 0.0f;

	/**
	 * The spannable string builder
	 */
	private SpannableStringBuilder mStringBuilder = new SpannableStringBuilder("");

	/**
	 * The Dynamic Layout
	 */
	private DynamicLayout mDynamicLayout = null;

	/**
	 * Show the progress Text
	 */
	private boolean mbShowProgress = false;

	/**
	 * The sweep angle or arc length
	 */
	private float mnArcLength = 0;

	/**
	 * The Value animator to start the transition animation of scaling down the text and finishing the circle
	 */
	private ValueAnimator mTransitionTextAndCircleValueAnimator = null;

	/**
	 * The Value animator to start the transition animation of increasing the line width to begin the State
	 * Animation
	 */
	private ValueAnimator mTransitionLineWidthValueAnimator = null;

	/**
	 * The Value animator to start the State Transition
	 */
	private ValueAnimator mTransitionToStateValueAnimator = null;

	/**
	 * The progress of the current transition animation
	 */
	private float mnTransitionProgress = 0.0f;

	/**
	 * The size of the Drawable
	 */
	private int mnSize = 0;

	/**
	 * The Ring Radius
	 */
	private int mnRingRadius = 0;

	/**
	 * The initial Innercircle radius
	 */
	private int mnInnerCircleRadius = 0;

	/**
	 * The Center of the Drawable
	 */
	private int mnViewCenter = 0;

	/**
	 * The current progress Radius based on the progress
	 */
	private float mnProgressRadius = 0.0f;

	/**
	 * The Line Width
	 */
	private float mnLineWidth = 0.0f;

	/**
	 * The download intimation complete listener
	 */
	private OnDownloadIntimationListener mOnDownloadIntimationListener = null;

	/**
	 * The Animation complete handler, so that a delay can be shown after the animation completes.
	 * This is so that the animation does not run too fast and break the UX
	 */
	private Handler mCompletionHandler = new Handler();


	//////////////////////////////////////// CLASS METHODS /////////////////////////////////////////

	/**
	 * Constructor to inflate the custom widget. The Android system calls the appropriate constructor
	 *
	 * @param context
	 * 		The context of the activity which acts as a parent to the widget
	 *
	 * @author Melvin Lobo
	 */
	public DashSpinner(Context context) {
		this(context, null);
	}

	/**
	 * Constructor to inflate the custom widget. The Android system calls the appropriate constructor
	 *
	 * @param context
	 * 		The context of the activity which acts as a parent to the widget
	 * @param attrs
	 * 		The custom attributes associated with this widget and defined in the xml
	 *
	 * @author Melvin Lobo
	 */
	public DashSpinner(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Constructors to inflate the custom widget. The Android system calls the appropriate constructor
	 *
	 * @param context
	 * 		The context of the activity which acts as a parent to the widget
	 * @param attrs
	 * 		The custom attributes associated with this widget and defined in the xml
	 * @param defStyle
	 * 		The default style to be applied
	 *
	 * @author Melvin Lobo
	 */
	public DashSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		/*
		 * Extract the attributes
		 */
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DashSpinner, 0, 0);

			mOuterRingColor = a.getColor(R.styleable.DashSpinner_outerRingColor, ContextCompat.getColor(context, android.R.color.holo_blue_dark));
			mInnerCircleSuccessColor = a.getColor(R.styleable.DashSpinner_innerCircleSuccessColor, ContextCompat.getColor(context, android.R.color.holo_green_light));
			mInnerCircleFailureColor = a.getColor(R.styleable.DashSpinner_innerCircleFailureColor, ContextCompat.getColor(context, android.R.color.holo_red_light));
			mInnerCircleUnknownColor = a.getColor(R.styleable.DashSpinner_innerCircleUnknownColor, ContextCompat.getColor(context, android.R.color.holo_orange_light));
			mArcColor = a.getColor(R.styleable.DashSpinner_arcColor, ContextCompat.getColor(context, android.R.color.white));
			mTextColorFrom = a.getColor(R.styleable.DashSpinner_textColorFrom, ContextCompat.getColor(context, android.R.color.black));
			mTextColorTo = a.getColor(R.styleable.DashSpinner_textColorTo, ContextCompat.getColor(context, android.R.color.white));
			mnIndeterminateStartPosition = a.getFloat(R.styleable.DashSpinner_arcStartPosition, ARC_START_POSITION);
			mnStartSpeed = a.getFloat(R.styleable.DashSpinner_arcSweepSpeed, DEFAULT_START_SPEED);
			mnArcWidth = a.getDimension(R.styleable.DashSpinner_arcWidth, d2x(DEFAULT_ARC_WIDTH));
			mnRingWidth = a.getDimension(R.styleable.DashSpinner_outerRingWidth, d2x(DEFAULT_RING_WIDTH));
			mnMaxTextSize = (int) a.getDimension(R.styleable.DashSpinner_maxProgressTextSize, d2x(DEFAULT_MAX_TEXT_SIZE));
			mbShowProgress = a.getBoolean(R.styleable.DashSpinner_showProgressText, false);
			mnArcLength = a.getFloat(R.styleable.DashSpinner_arcLength, DEFAULT_ARC_LENGTH);
			a.recycle();
		}

		//Initialize the Text Paint
		mTextPaint.setTextSize(mnMaxTextSize);
		mTextPaint.setColor(mTextColorFrom);
		mTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

		setLayerType(View.LAYER_TYPE_SOFTWARE, mPaint);
	}

	/**
	 * Set the Download Intimation Listener
	 *
	 * @param listener
	 * 		The Download Intimation Listener
	 *
	 * @author Melvin Lobo
	 */
	public void setOnDownloadIntimationListener(OnDownloadIntimationListener listener) {
		mOnDownloadIntimationListener = listener;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		// Initialize the values;
		initializeValues();

		// Build a new Dynamic Layout with the available width since we can only provide width when the dynamic layout is created
		mDynamicLayout = new DynamicLayout(mStringBuilder, mStringBuilder, mTextPaint, w, Layout.Alignment.ALIGN_CENTER, 1.0f, 1.0f, true);
	}

	/**
	 * The onDraw function. see {@link View#onDraw(Canvas)}. This function does the following:
	 * 1. Draws the outer ring
	 * 2. Draws the arc that moves around the ring and updates its position
	 * 3. Draws an inner circle which grows with the progress
	 * 4. Draws a text with the current progress value which grows to its max size set by the user
	 *
	 * @param canvas
	 * 		THe canvas to draw on
	 *
	 * @author Melvin Lobo
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		/*
		 * Reset previous values
		 */
		resetPaint();

		/*
		 * The Outer Ring
		 */
		drawOuterRing(canvas);

		/*
		 * Draw the Inner circle. The mnProgressRadius is updated here
		 */
		drawInnerCircle(canvas);

		/*
		 * Draw the State Content in the center
		 */
		drawStateContent(canvas);

		/*
		 * Draw the indeterminate Arc
		 */
		drawArc(canvas);
	}

	/**
	 * Reset the Paint variable
	 *
	 * @author Melvin Lobo
	 */
	private void resetPaint() {
		mPaint.reset();
		mPaint.setAntiAlias(true);
	}

	/**
	 * Reset the calculated parameters
	 *
	 * @author Melvin Lobo
	 */
	public void resetValues() {
		mnSize = 0;
		mnRingRadius = 0;
		mnInnerCircleRadius = 0;
		mnViewCenter = 0;
		mnProgress = 0.0f;
		mnTransitionProgress = 0.0f;
		mTransitionLineWidthValueAnimator = null;
		mTransitionTextAndCircleValueAnimator = null;
		mTransitionToStateValueAnimator = null;
		mCurrentDashMode = DASH_MODE.NONE;
		mNextDashMode = DASH_MODE.NONE;
		initializeValues();
	}

	/**
	 * Initialize the Calculated Values
	 *
	 * @author Melvin Lobo
	 */
	private void initializeValues() {
		mnSize = Math.min(getMeasuredHeight(), getMeasuredWidth());
		mnRingRadius = (int)(mnSize - mnRingWidth) / 2;
		mnInnerCircleRadius = (int)(mnSize - (mnRingWidth * 2)) / 2;
		mnViewCenter = mnSize / 2;
		mnLineWidth = STATUS_SYMBOL_WIDTH_PERCENT * mnSize;
	}


	/**
	 * Draw the outer ring
	 * @param canvas
	 * 		The canvas to draw on
	 *
	 * @author Melvin Lobo
	 */
	private void drawOuterRing(Canvas canvas) {
		//Draw the outer ring
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(mnRingWidth);
		mPaint.setColor(mOuterRingColor);
		canvas.drawCircle(mnViewCenter, mnViewCenter, mnRingRadius, mPaint);
	}

	/**
	 * Draw the Inner circle based on the Current Dash Mode. We have to draw for every mode because
	 * the Inner circle is present in all modes
	 *
	 * @param canvas
	 * 		The canvas to draw on
	 *
	 * @author Melvin Lobo
	 */
	private void drawInnerCircle(Canvas canvas) {
		float nDrawRadius = 0.0f;
		mPaint.setStyle(Paint.Style.FILL);
		switch (mCurrentDashMode) {
			case DOWNLOAD: {
				/*
				 * This circle will grow with the progress and its alpha will change
				 * accordingly. Since we are using only one Paint object, after drawing the circle, we move back
				 * the alpha to 1
				 */
				mPaint.setColor(mInnerCircleSuccessColor);
				mPaint.setAlpha(getInnerCircleAlpha());        // Alpha according to the progress (0..255)

				float nCurrentRadius = mnInnerCircleRadius * mnProgress;
				mnProgressRadius = (nCurrentRadius < mnInnerCircleRadius) ? nCurrentRadius : mnInnerCircleRadius;
				nDrawRadius = mnProgressRadius;
			}
			break;
			case TRANSITION_TEXT_AND_CIRCLE:
			case TRANSITION_LINE: {
				/*
				 * Draw the transition from the already existing radius and alpha to the Error / Unknown Circle
				 */
				if (mNextDashMode.equals(DASH_MODE.FAILURE) || mNextDashMode.equals(DASH_MODE.UNKNOWN)) {
					mPaint.setColor((mNextDashMode.equals(DASH_MODE.FAILURE) ? mInnerCircleFailureColor : mInnerCircleUnknownColor));

					if (mCurrentDashMode.equals(DASH_MODE.TRANSITION_TEXT_AND_CIRCLE)) {
						/*
						 * Once we move to transition. specially for Failure and Unknown we need to do the following values
						 * to start from:
						 * 1. The transitionary radius for drawing the remainder of the circle will depend on mnProgress
						 *    This is because, if we get a failure or unknown error when the download fails, we will transition
						 *    from the current size of the Inner circle radius (based on the download progress). But since the transition progress for this animation
						 *    begins with 1.0f and ends at 0.0f, we first inverse mnTransitionProgress, get the radii difference between
						 *    the current radius and the final radius and apply the inverse of mnTransitionProgress to this value
						 * 2. The color for failure will have to be set, but the blending will begin from where mnProgress left off
						 *    So, we will calculate the remaining difference from the Current value where it has failed to the final
						 *    alpha value (1.0) and apply the inverse values of mnTransitionProgress to this difference
						 */
						float nInverseTransition = 1 - mnTransitionProgress;

						nDrawRadius = mnProgressRadius /*The previous radius, if any*/ +
									  ((mnInnerCircleRadius - mnProgressRadius) * nInverseTransition) /*The differential transitional radius*/;

						mPaint.setAlpha(getInnerCircleAlpha() /*The previous Alpha, if any*/+
										((int) ((MAX_ALPHA - getInnerCircleAlpha()) * nInverseTransition))) /*The differential transitional alpha*/;
					}
					/*Draw the circle with UNKNOWN / FAILURE color and full alpha*/
					else {
						mPaint.setAlpha(MAX_ALPHA);
						nDrawRadius = mnInnerCircleRadius;
					}
				}
				/*
				 * Else, just draw the success circle with full alpha
				 */
				else {
					mPaint.setColor(mInnerCircleSuccessColor);
					mPaint.setAlpha(MAX_ALPHA);        // Alpha according to the progress (0..255)
					nDrawRadius = mnInnerCircleRadius;
				}
			}
			break;
			case SUCCESS: {
				/*
				 * Draw for Failure with Full Alpha
				 */
				mPaint.setColor(mInnerCircleSuccessColor);
				mPaint.setAlpha(MAX_ALPHA);
				nDrawRadius = mnInnerCircleRadius;
			}
			break;
			case FAILURE: {
				/*
				 * Draw for Failure with Full Alpha
				 */
				mPaint.setColor(mInnerCircleFailureColor);
				mPaint.setAlpha(MAX_ALPHA);
				nDrawRadius = mnInnerCircleRadius;
			}
			break;
			case UNKNOWN: {
				/*
				 * Draw for Unknown with Full Alpha
				 */
				mPaint.setColor(mInnerCircleUnknownColor);
				mPaint.setAlpha(MAX_ALPHA);
				nDrawRadius = mnInnerCircleRadius;
			}
			break;
		}
		canvas.drawCircle(mnViewCenter, mnViewCenter, nDrawRadius, mPaint);
		mPaint.setAlpha(MAX_ALPHA);
	}

	/**
	 * Get the alpha values based on the progress (0..255)
	 * @return
	 * 		The alpha value of the color based on the progress
	 *
	 * @author Melvin Lobo
	 */
	private int getInnerCircleAlpha() {
		int nCurrAlpha =  (int)(MAX_ALPHA * mnProgress);
		nCurrAlpha = (nCurrAlpha < 0) ? 0 : ((nCurrAlpha > MAX_ALPHA) ? MAX_ALPHA : nCurrAlpha);
		return nCurrAlpha;
	}

	/**
	 * Draw the state content. The state content can be the percent value of the progress (If the user
	 * elects to draw it), the transition to other states and that state content themselves (SUCCESS,
	 * FAILURE OR UNKNOWN). We have to draw for all modes, as the Content is a part of all modes
	 *
	 * @param canvas
	 * 		The canvas to draw on
	 *
	 * @author Melvin Lobo
	 */
	private void drawStateContent(Canvas canvas) {
		float appropriateFontSize = 0.0f;
		switch (mCurrentDashMode) {
				/*
				 * The DOWNLOAD and TRANSITION_TEXT_AND_CIRCLE modes are similar except that in the
				 * TRANSITION_TEXT_AND_CIRCLE mode, the text is scaling down instead of up
				 * and we have complete alpha
				 * Note (Mode case TRANSITION_TEXT_AND_CIRCLE):
				 * Draw the text till the scale down reaches TEXT_SCALE_DOWN_PERCENT_VALUE % of its size. After that draw a circle
				 * for the rest of the TEXT_SCALE_DOWN_PERCENT_VALUE % of the animation time. Then the next animation of growing the line starts
				 */
			case DOWNLOAD:
			case TRANSITION_TEXT_AND_CIRCLE: {
				/*
				 * Draw the small Circle instead of text
				 */
				if (mCurrentDashMode.equals(DASH_MODE.TRANSITION_TEXT_AND_CIRCLE) && (mnTransitionProgress < (TRANSITION_CAT_START_VAL * TEXT_SCALE_DOWN_PERCENT_VALUE))) {
					/*
					 * Draw a circle till the next animation starts
					 */
					mPaint.setColor(mTextColorTo);
					canvas.drawCircle(mnViewCenter, mnViewCenter, d2x(STATE_LINE_STROKE) / 2, mPaint);
				}
				/*
				 * Draw the text
				 */
				else {
					//Draw the download progress if the User wants it
					if(mbShowProgress) {
						/*
						 * The Percentage Text. Calculate the size of the text as per the center circle till it reaches the size
						 * that the user desires
						 */
						float nTextWidth = ((mCurrentDashMode.equals(DASH_MODE.DOWNLOAD)) ? (mnProgressRadius * 2) : (mnProgressRadius * mnTransitionProgress * 2)) - d2x(TEXT_PADDING);
						appropriateFontSize = getSingleLineTextSize(msProgressText, mTextPaint, nTextWidth, 0.0f, mnMaxTextSize, 0.5f, getResources().getDisplayMetrics());
						msProgressText = (int) (mnProgress * 100) + "%";        //The percentage value string
						mTextPaint.setTextSize(appropriateFontSize);
						mTextPaint.setColor((mCurrentDashMode.equals(DASH_MODE.DOWNLOAD)) ? blendColors(mTextColorFrom, mTextColorTo, mnProgress) : mTextColorTo);

						/*
						 * Change the spannable string builder text assigned to the dynamic layout so that the width is recalculated,
						 * translate the canvas to the center, draw the text and restore the canvas back to its position
						 */
						mStringBuilder.replace(0, mStringBuilder.length(), msProgressText);
						canvas.save();
						canvas.translate(mnViewCenter - (mDynamicLayout.getWidth() / 2), mnViewCenter - (mDynamicLayout.getHeight() / 2));
						mDynamicLayout.draw(canvas);
						canvas.restore();
					}
				}
			}
			break;
			case TRANSITION_LINE: {
				mPaint.setColor(mTextColorTo);
				mPaint.setStrokeWidth(d2x(STATE_LINE_STROKE));
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setStrokeCap(Paint.Cap.ROUND);
				float nDiffLength = mnLineWidth * mnTransitionProgress;        // The line length based on the transition progress

				/*
				 * The line will be at different positions based on the Status Mode (SUCCESS, FAILURE
				 * OR UNKNOWN). This is because we would need the line slightly offset in the X position
				 * from the center for a SUCCESS as the dynamics of it converting to a tick would be different.
				 * The tick "joint" would be a shorter ratio of the entire line
				 */
				if (mNextDashMode.equals(DASH_MODE.SUCCESS)) {
					float nStart = mnViewCenter - (TICK_SHORT_ARM_RATIO_PERCENT * nDiffLength);        // The short arm ratio of the line length
					float nEnd = mnViewCenter + (TICK_LONG_ARM_RATIO_PERCENT * nDiffLength);            // The long arm ratio of the line length
					canvas.drawLine(nStart, mnViewCenter, nEnd, mnViewCenter, mPaint);
				}
				else {
					canvas.drawLine(mnViewCenter - (nDiffLength / 2), mnViewCenter, mnViewCenter + (nDiffLength / 2), mnViewCenter, mPaint);
				}
			}
			break;
			case SUCCESS: {
				/*
				 * Set the Paint up to draw the Success Tick
				 */
				mPaint.setColor(mTextColorTo);
				mPaint.setStrokeWidth(d2x(STATE_LINE_STROKE));
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setStrokeCap(Paint.Cap.ROUND);

				/*
				 *   		  /
				 * 			\/
				 * 			^
				 * 		Tick Joint
				 *
				 * We first draw the short arm of the tick at a downward angle of  TICK_SHORT_ARM_ANGLE_RADIANS.
				 * The Mathematical equation for finding the end point to draw such a line is:
				 *
				 * X Position = Start Point + Length of the Line * cos( Angle that the line has to be drawn on)
				 * Y Position = Start Point + Length of the Line * sin( Angle that the line has to be drawn on)
				 *
				 * In order for the tick joint to be shown around the Y pos of center of the View, the short arm will have to end
				 * around that position. So, we will need to calculate the X co-ordinate of start position for the short arm,
				 * based on the end position (which will be fixed since we want it to be based around the end position,
				 * View Center) of the short arm. So we reverse the above equation to Calculate the start position.
				 * X Postion Start = X Postion End - Length of the Line * cos( Angle that the line has to be drawn on)
				 * We than use the above calculations to draw the long arm at an angle TICK_LONG_ARM_ANGLE_RADIANS
				 * from the endpoint of the shorter arm.
				 * We transition the angles using the interpolator to animate the tick forming from the line
				 */
				float nShortArmLength = TICK_SHORT_ARM_RATIO_PERCENT * mnLineWidth;
				float nLongArmLength = TICK_LONG_ARM_RATIO_PERCENT * mnLineWidth;
				float nStartShortArmX = (float) (mnViewCenter - nShortArmLength * Math.cos(Math.toRadians(TICK_SHORT_ARM_ANGLE_RADIANS * mnTransitionProgress)));
				float nEndShortArmY = (float) (mnViewCenter + nShortArmLength * Math.sin(Math.toRadians(TICK_SHORT_ARM_ANGLE_RADIANS * mnTransitionProgress)));
				float nEndLongArmX = (float) (nStartShortArmX + nLongArmLength * Math.cos(Math.toRadians(TICK_LONG_ARM_ANGLE_RADIANS * mnTransitionProgress)));
				float nEndLongArmY = (float) (nEndShortArmY + nLongArmLength * Math.sin(Math.toRadians(TICK_LONG_ARM_ANGLE_RADIANS * mnTransitionProgress)));
				canvas.drawLine(nStartShortArmX, mnViewCenter, mnViewCenter, nEndShortArmY, mPaint);
				canvas.drawLine(mnViewCenter, nEndShortArmY, nEndLongArmX, nEndLongArmY, mPaint);
			}
			break;
			case FAILURE: {
				/*
				 * Set the Paint up to draw the Failure cross
				 */
				mPaint.setColor(mTextColorTo);
				mPaint.setStrokeWidth(d2x(STATE_LINE_STROKE));
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setStrokeCap(Paint.Cap.ROUND);

				/*
				 * The arm length for the cross is Half of mnLineLength. We will transition each arm
				 * with angles (ARM_ANGLE, -ARM_ANGLE, 180 - ARM_ANGLE, 180 + ARM_ANGLE) for each quadrant
				 * beginning from the horizontal X-Axis. We calculate the end points for the lines in each
				 * quadrant based on the Equations:
				 *
				 * X Position = Start Point + Length of the Line * cos( Angle that the line has to be drawn on)
				 * Y Position = Start Point + Length of the Line * sin( Angle that the line has to be drawn on)
				 */
				float nArmLength = mnLineWidth / 2;
				float nQuadFourX = (float) (mnViewCenter + nArmLength * Math.cos(Math.toRadians(ARM_ANGLE * mnTransitionProgress)));
				float nQuadFourY = (float) (mnViewCenter + nArmLength * Math.sin(Math.toRadians(ARM_ANGLE * mnTransitionProgress)));
				float nQuadOneX = (float) (mnViewCenter + nArmLength * Math.cos(Math.toRadians(-ARM_ANGLE * mnTransitionProgress)));
				float nQuadOneY = (float) (mnViewCenter + nArmLength * Math.sin(Math.toRadians(-ARM_ANGLE * mnTransitionProgress)));
				float nQuadThreeX = (float) (mnViewCenter + nArmLength * Math.cos(Math.toRadians(180 - (ARM_ANGLE * mnTransitionProgress))));
				float nQuadThreeY = (float) (mnViewCenter + nArmLength * Math.sin(Math.toRadians(180 - (ARM_ANGLE * mnTransitionProgress))));
				float nQuadTwoX = (float) (mnViewCenter + nArmLength * Math.cos(Math.toRadians(180 - (-ARM_ANGLE * mnTransitionProgress))));
				float nQuadTwoY = (float) (mnViewCenter + nArmLength * Math.sin(Math.toRadians(180 - (-ARM_ANGLE * mnTransitionProgress))));

				canvas.drawLine(mnViewCenter, mnViewCenter, nQuadOneX, nQuadOneY, mPaint);
				canvas.drawLine(mnViewCenter, mnViewCenter, nQuadTwoX, nQuadTwoY, mPaint);
				canvas.drawLine(mnViewCenter, mnViewCenter, nQuadThreeX, nQuadThreeY, mPaint);
				canvas.drawLine(mnViewCenter, mnViewCenter, nQuadFourX, nQuadFourY, mPaint);
			}
			break;
			case UNKNOWN: {
				/*
				 * Set the Paint up to draw the Unknown Exclamation
				 */
				mPaint.setColor(mTextColorTo);
				mPaint.setStrokeWidth(d2x(STATE_LINE_STROKE));
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setStrokeCap(Paint.Cap.ROUND);

				/*
				 * For Unknown, we just draw a line and a dot below it. The canvas is rotated for transition
				 * and the distance between the line and dot increases to its final value.
				 */
				float nDotRadius = STATE_LINE_STROKE / 2;
				float nDotDistance = d2x(UNKNOWN_DOT_DISTANCE);
				float nArmLength = mnLineWidth / 2;
				float nQuadOneX = (float) (mnViewCenter + nArmLength * Math.cos(Math.toRadians(-UNKNOWN_ROTATION_ANGLE * mnTransitionProgress)));
				float nQuadOneY = (float) (mnViewCenter + nArmLength * Math.sin(Math.toRadians(-UNKNOWN_ROTATION_ANGLE * mnTransitionProgress)));
				float nQuadThreeX = (float) (mnViewCenter + nArmLength * Math.cos(Math.toRadians(180 - (UNKNOWN_ROTATION_ANGLE * mnTransitionProgress))));
				float nQuadThreeY = (float) (mnViewCenter + nArmLength * Math.sin(Math.toRadians(180 - (UNKNOWN_ROTATION_ANGLE * mnTransitionProgress))));
				float nDotX = (float) (mnViewCenter + (nArmLength + (nDotDistance * mnTransitionProgress)) * Math.cos(Math.toRadians(180 - (UNKNOWN_ROTATION_ANGLE * mnTransitionProgress))));
				float nDotY = (float) (mnViewCenter + (nArmLength + (nDotDistance * mnTransitionProgress)) * Math.sin(Math.toRadians(180 - (UNKNOWN_ROTATION_ANGLE * mnTransitionProgress))));

				canvas.drawLine(mnViewCenter, mnViewCenter, nQuadOneX, nQuadOneY, mPaint);
				canvas.drawLine(mnViewCenter, mnViewCenter, nQuadThreeX, nQuadThreeY, mPaint);
				canvas.drawCircle(nDotX, nDotY, nDotRadius, mPaint);
			}
			break;
		}
	}

	/**
	 * Draw the arc around the ring only for the DOWNLOAD mode
	 *
	 * @param canvas
	 * 		The canvas to draw on
	 *
	 * @author Melvin Lobo
	 */
	private void drawArc(Canvas canvas) {
		/*
		 * For every progress increase of 1%, decrease speed by 1%
		 * The goal of the progress is to reach 1.0, while that of the speed is to reach 0.0
		 */
		if(mCurrentDashMode.equals(DASH_MODE.DOWNLOAD)) {
			mnIndeterminateStartPosition += (1 - mnProgress) * mnStartSpeed;
			if ((mnIndeterminateStartPosition > CIRCULAR_FACTOR) || (mnIndeterminateStartPosition < 0)) {
				mnIndeterminateStartPosition = 0;
			}

			/*
			 * The View rect. We need this for height and width calculations
			 */
			Rect currRect = new Rect();
			getLocalVisibleRect(currRect);
			float nRingBoundaryInner = mnRingRadius - (mnRingWidth / 2) - (mnArcWidth / 2);
			mArcRect.set(mnViewCenter - nRingBoundaryInner, mnViewCenter - nRingBoundaryInner, mnViewCenter + nRingBoundaryInner, mnViewCenter + nRingBoundaryInner);
			mPaint.setColor(mArcColor);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(mnArcWidth);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			canvas.drawArc(mArcRect, mnIndeterminateStartPosition, mnArcLength, false, mPaint);
		}
	}

	/**
	 * Recursive binary search to find the best size for the text.
	 *
	 * Adapted from https://github.com/grantland/android-autofittextview
	 *
	 * @author Melvin Lobo
	 */
	public static float getSingleLineTextSize(String text, TextPaint paint, float targetWidth, float low, float high, float precision,
											  DisplayMetrics metrics) {

		/*
		 * Find the mid
 		 */
		final float mid = (low + high) / 2.0f;

		/*
		 * Get the maximum text width for the Mid
 		 */
		paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, mid, metrics));
		final float maxLineWidth = paint.measureText(text);

		/*
		 * If the value is not close to precision, based on if its greater than or less than the target width,
		 * we move to either side of the scle divided by the mid and repeat the process again
 		 */
		if ((high - low) < precision) {
			return low;
		} else if (maxLineWidth > targetWidth) {
			return getSingleLineTextSize(text, paint, targetWidth, low, mid, precision, metrics);
		} else if (maxLineWidth < targetWidth) {
			return getSingleLineTextSize(text, paint, targetWidth, mid, high, precision, metrics);
		} else {
			return mid;
		}
	}

	/**
	 * Convert dip to pixels
	 *
	 * param size The size to be converted
	 *
	 * @author Melvin Lobo
	 */
	private float d2x(float size) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, getContext().getResources().getDisplayMetrics());
	}

	/**
	 * Get the color blend based on the ratio of progress. Blend each R/G/B streams
	 *
	 * @param nFromColor
	 * 		The color from which the blending takes place
	 * @param nToColor
	 * 		The color to which it should blend
	 * @param nProgress
	 * 		THe current ratio of conversion (same as the progress)
	 *
	 * @author Melvin Lobo
	 */
	private int blendColors(int nFromColor, int nToColor, float nProgress) {
		final float nInverseProgress = 1f - nProgress;

		final float r = Color.red(nToColor) * nProgress + Color.red(nFromColor) * nInverseProgress;
		final float g = Color.green(nToColor) * nProgress + Color.green(nFromColor) * nInverseProgress;
		final float b = Color.blue(nToColor) * nProgress + Color.blue(nFromColor) * nInverseProgress;

		return Color.rgb((int) r, (int) g, (int) b);
	}

	/**
	 * Set the progress. The View will invalidate itself on each call.
	 * Do this only if the Spinner is downloading or has just been initialized
	 *
	 * @param nProgress
	 * 		The float value of progress between 0 and 1
	 *
	 * @author Melvin Lobo
	 */
	public void setProgress(float nProgress) {
		if(mCurrentDashMode.equals(DASH_MODE.NONE) || mCurrentDashMode.equals(DASH_MODE.DOWNLOAD)) {
			mCurrentDashMode = DASH_MODE.DOWNLOAD;
			mnProgress = (nProgress < 0.0f) ? 0.0f : ((nProgress > 1.0f) ? 1.0f : nProgress);
			postInvalidate();
		}
	}

	/**
	 * Show Success
	 *
	 * @author Melvin Lobo
	 */
	public void showSuccess() {
		mCurrentDashMode = DASH_MODE.TRANSITION_TEXT_AND_CIRCLE;
		mNextDashMode = DASH_MODE.SUCCESS;
		startCircleAndTextTransitionAnimation();
	}

	/**
	 * Show Success
	 *
	 * @author Melvin Lobo
	 */
	public void showFailure() {
		mCurrentDashMode = DASH_MODE.TRANSITION_TEXT_AND_CIRCLE;
		mNextDashMode = DASH_MODE.FAILURE;
		startCircleAndTextTransitionAnimation();
	}

	/**
	 * Show Success
	 *
	 * @author Melvin Lobo
	 */
	public void showUnknown() {
		mCurrentDashMode = DASH_MODE.TRANSITION_TEXT_AND_CIRCLE;
		mNextDashMode = DASH_MODE.UNKNOWN;
		startCircleAndTextTransitionAnimation();
	}

	/**
	 * Start Transition Animation
	 *
	 * @author Melvin Lobo
	 */
	private void startCircleAndTextTransitionAnimation() {

		if(mTransitionTextAndCircleValueAnimator == null) {
			mTransitionTextAndCircleValueAnimator = ValueAnimator.ofFloat(mnTransitionProgress);
			mTransitionTextAndCircleValueAnimator.setFloatValues(TRANSITION_CAT_START_VAL, TRANSITION_CAT_END_VAL);
			mTransitionTextAndCircleValueAnimator.setDuration(TRANSITION_ANIM_DURATION);
			mTransitionTextAndCircleValueAnimator.setInterpolator(new DecelerateInterpolator());
			mTransitionTextAndCircleValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					mnTransitionProgress = (float) animation.getAnimatedValue();
					postInvalidate();
				}
			});

			mTransitionTextAndCircleValueAnimator.addListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {

				}

				@Override
				public void onAnimationEnd(Animator animation) {
					startLineScaleTransitionAnimation();
				}

				@Override
				public void onAnimationCancel(Animator animation) {

				}

				@Override
				public void onAnimationRepeat(Animator animation) {

				}
			});
		}
		else if(mTransitionTextAndCircleValueAnimator.isRunning()){
			mTransitionTextAndCircleValueAnimator.cancel();
		}

		mTransitionTextAndCircleValueAnimator.start();
	}

	/**
	 * Start Transition Animation to Scale the line
	 *
	 * @author Melvin Lobo
	 */
	private void startLineScaleTransitionAnimation() {
		//Start the second transition for the line. We can use the same member variable  for transition progress
		mCurrentDashMode = DASH_MODE.TRANSITION_LINE;
		if (mTransitionLineWidthValueAnimator == null) {
			mTransitionLineWidthValueAnimator = ValueAnimator.ofFloat(mnTransitionProgress);
			mTransitionLineWidthValueAnimator.setFloatValues(TRANSITION_CAT_END_VAL, TRANSITION_CAT_START_VAL);
			mTransitionLineWidthValueAnimator.setDuration(TRANSITION_ANIM_DURATION);
			mTransitionLineWidthValueAnimator.setInterpolator(new DecelerateInterpolator());
			mTransitionLineWidthValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					mnTransitionProgress = (float) animation.getAnimatedValue();
					postInvalidate();
				}
			});

			mTransitionLineWidthValueAnimator.addListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {

				}

				@Override
				public void onAnimationEnd(Animator animation) {
					startStateTransition();
				}

				@Override
				public void onAnimationCancel(Animator animation) {

				}

				@Override
				public void onAnimationRepeat(Animator animation) {

				}
			});
		}
		else if (mTransitionLineWidthValueAnimator.isRunning()) {
			mTransitionLineWidthValueAnimator.cancel();
		}

		mTransitionLineWidthValueAnimator.start();
	}

	/**
	 * Start Transition Animation to state
	 *
	 * @author Melvin Lobo
	 */
	private void startStateTransition() {
		//mTransitionToStateValueAnimator
		//Start the state transition for the line. We can use the same member variable for transition progress
		mCurrentDashMode = mNextDashMode;
		mNextDashMode = DASH_MODE.NONE;

		if (mTransitionToStateValueAnimator == null) {
			mTransitionToStateValueAnimator = ValueAnimator.ofFloat(mnTransitionProgress);
			mTransitionToStateValueAnimator.setFloatValues(TRANSITION_CAT_END_VAL, TRANSITION_CAT_START_VAL);
			mTransitionToStateValueAnimator.setDuration(TRANSITION_ANIM_DURATION);
			mTransitionToStateValueAnimator.setInterpolator(new DecelerateInterpolator());
			mTransitionToStateValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					mnTransitionProgress = (float) animation.getAnimatedValue();
					postInvalidate();
				}
			});

			mTransitionToStateValueAnimator.addListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					mCompletionHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if(mOnDownloadIntimationListener != null)
								mOnDownloadIntimationListener.onDownloadIntimationDone(mCurrentDashMode);
						}
					}, TRANSITION_ANIM_DURATION);

				}

				@Override
				public void onAnimationCancel(Animator animation) {

				}

				@Override
				public void onAnimationRepeat(Animator animation) {

				}
			});
		}
		else if (mTransitionToStateValueAnimator.isRunning()) {
			mTransitionToStateValueAnimator.cancel();
		}

		mTransitionToStateValueAnimator.start();
	}

	//////////////////////////////////////// INTERFACE /////////////////////////////////////////

	/**
	 * Interface to listen after the Dash spinner has completed showing animations to the user.
	 * This interface exists so that the UX is not gutted and a seamless experience of the complete
	 * animation is shown
	 *
	 * @author Melvin Lobo
	 */
	public interface OnDownloadIntimationListener {
		/**
		 * Notify that the visual intimation to the user is complete
		 *
		 * @param dashMode
		 * 		The status that was shown to the user
		 *
		 * @author Melvin Lobo
		 */
		void onDownloadIntimationDone(DASH_MODE dashMode);
	}
}
