package com.example.chatbuddy;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordCloud {
    private Map<String, Integer> wordMap = new HashMap<>();
    private int dimenWidth = 480;
    private int dimenHeight = 640;
    private int defaultWordColor = Color.BLACK;
    private int defaultBackgroundColor = Color.TRANSPARENT;
    private int calculatedHeight = 0;
    private int maxFontSize = 40;
    private int minFontSize = 10;
    private int maxColorAlphaValue = 255;
    private int minColorAlphaValue = 50;
    private int paddingX = 1;
    private int paddingY = 1;
    private boolean wordColorOpacityAuto = false;
    private Typeface customTypeFace = null;
    private boolean boundingYAxis = true;

    public WordCloud() {
        // empty constructor
    }

    public WordCloud(int dimenWidth, int dimenHeight) {
        this.dimenWidth = dimenWidth;
        this.dimenHeight = dimenHeight;
    }

    public WordCloud(Map<String, Integer> wordMap, int dimenWidth, int dimenHeight) {
        this.wordMap.putAll(wordMap);
        this.dimenWidth = dimenWidth;
        this.dimenHeight = dimenHeight;
    }

    public WordCloud(Map<String, Integer> wordMap, int dimenWidth, int dimenHeight,
                     int defaultWordColor, int defaultBackgroundColor) {
        this.wordMap.putAll(wordMap);
        this.dimenWidth = dimenWidth;
        this.dimenHeight = dimenHeight;
        this.defaultWordColor = defaultWordColor;
        this.defaultBackgroundColor = defaultBackgroundColor;
    }

    /*
    -1: Failure
    0: too large
    1: Normal, fitting, perfect
     */
    private int fit(List<Word> wordList) {
        if (wordList.size() > 0) {
            calculatedHeight = 0;

            int tempX = wordList.get(0).getWordRect().width();
            int tempY = wordList.get(0).getWordRect().height();
            for(int h = 1; h < wordList.size(); h++) {
                if (tempX + wordList.get(h).getWordRect().width() + paddingX > dimenWidth) {
                    wordList.get(h).getWordRect().offsetTo(0, tempY + paddingY);
                    tempY += (wordList.get(h).getWordRect().height() + paddingY);
                    tempX = wordList.get(h).getWordRect().width();
                } else {
                    wordList.get(h).getWordRect().offsetTo(tempX + paddingX, 0);
                    tempX += (wordList.get(h).getWordRect().width() + paddingX);
                }
                Rect intersectRect = isWordIntersectWithOthers(wordList.get(h), wordList);
                while (intersectRect != null) {
                    int diffY = Math.abs(intersectRect.bottom - wordList.get(h).getWordRect().top);
                    wordList.get(h).getWordRect().offsetTo(wordList.get(h).getWordRect().left,
                            wordList.get(h).getWordRect().top + diffY);
                    intersectRect = isWordIntersectWithOthers(wordList.get(h), wordList);
                }
                wordList.get(h).getWordRect().offsetTo(wordList.get(h).getWordRect().left,
                        wordList.get(h).getWordRect().top + paddingY);

                if (wordList.get(h).getWordRect().bottom > calculatedHeight) {
                    calculatedHeight = wordList.get(h).getWordRect().bottom;
                }
            }
            if (calculatedHeight > dimenHeight) {
                // it means its too large to fit so decrease font size
                return 0;
            } else {
                // it means its perfectly fit or too small so increase font size or align to center
                return 1;
            }
        }
        return -1;
    }

    private Rect isWordIntersectWithOthers(Word w, List<Word> wordList) {
        if (wordList.size() > 0) {
            for (Word wd: wordList) {
                if (!w.equals(wd) && Rect.intersects(w.getWordRect(), wd.getWordRect())) {
                    return wd.getWordRect();
                }
            }
        }
        return null;
    }

    public Bitmap generate() { // TODO shuffle wordlist
        if (wordMap.size() == 0) {
            return null;
        }

        List<Word> finalWordList = new ArrayList<>();

        List<Integer> countList = new ArrayList<>(wordMap.values());
        Collections.sort(countList, Collections.reverseOrder());
        int largestWordCnt = countList.get(0);

        for (Map.Entry<String, Integer> entry : wordMap.entrySet()) {
            float calculatedTextSize = getTextSize(entry.getValue(),largestWordCnt,
                    this.maxFontSize, this.minFontSize);

            if (wordColorOpacityAuto) {
                int calculatedAlphaVal = getTextColorAlpha(entry.getValue(), largestWordCnt,
                        this.maxColorAlphaValue, this.minColorAlphaValue);

                if (customTypeFace != null) {
                    finalWordList.add(new Word(entry.getKey(), entry.getValue(), calculatedTextSize, customTypeFace,
                            defaultWordColor, calculatedAlphaVal));
                } else {
                    finalWordList.add(new Word(entry.getKey(), entry.getValue(), calculatedTextSize,
                            defaultWordColor, calculatedAlphaVal));
                }
            } else {
                if (customTypeFace != null) {
                    finalWordList.add(new Word(entry.getKey(), entry.getValue(), calculatedTextSize, customTypeFace,
                            defaultWordColor));
                } else {
                    finalWordList.add(new Word(entry.getKey(), entry.getValue(), calculatedTextSize,
                            defaultWordColor));
                }
            }
        }

        for (int newMaxFontSize = this.maxFontSize - 1; newMaxFontSize > this.minFontSize;
             newMaxFontSize--) {
            int fitResult = fit(finalWordList);
            if (!boundingYAxis) {
                break;
            }
            if (fitResult == 1) {
                break;
            } else if (fitResult == 0) {
                for (Word w: finalWordList) {
                    float calculatedNewTextSize = getTextSize(w.getWordCount(),largestWordCnt,
                            newMaxFontSize, this.minFontSize);
                    w.changeTextSize(calculatedNewTextSize);
                }
            } else {
                return null;
            }
        }

        // TODO exception here: calculatedHeight is becoming 0 sometimes
        // Hotfix: if it is 0, assign dimenHeight to calculatedHeight, IT IS WORKAROUND, remove when its fully fixed
        if (calculatedHeight <= 0) {
            calculatedHeight = dimenHeight;
        }

        // draw it

        Bitmap intermediateBmp =  Bitmap.createBitmap(dimenWidth,calculatedHeight,
                Bitmap.Config.ARGB_8888);
        Canvas intermediateCnv = new Canvas(intermediateBmp);
        intermediateCnv.drawColor(Color.TRANSPARENT);

        for (Word wordWillBeDrawed: finalWordList) {
            intermediateCnv.drawText(wordWillBeDrawed.getWord(),
                    wordWillBeDrawed.getX(),
                    wordWillBeDrawed.getY(),
                    wordWillBeDrawed.getWordPaint());
        }

        if (!boundingYAxis) {
            return intermediateBmp;
        }

        Bitmap retBmp = Bitmap.createBitmap(dimenWidth,dimenHeight,Bitmap.Config.ARGB_8888);
        Canvas retBmpCnv = new Canvas(retBmp);
        retBmpCnv.drawColor(defaultBackgroundColor); // background

        retBmpCnv.drawBitmap(intermediateBmp,
                (retBmp.getWidth() - intermediateBmp.getWidth()) / 2,
                (retBmp.getHeight() - intermediateBmp.getHeight()) / 2,
                new Paint());

        // TODO maybe call intermediateBmp.recycle() since its no more used

        return retBmp;
    }

    private float getTextSize(int wordCount, int largestWordCount,
                              int maxFontSize, int minFontSize) {
        return (float) (largestWordCount > 1 ? Math.log(wordCount) / Math.log(largestWordCount) *
                (maxFontSize - minFontSize) + minFontSize : minFontSize);
    }

    private int getTextColorAlpha(int wordCount, int largestWordCount,
                                  int maxAlphaVal, int minAlphaVal) {

        return (int) (largestWordCount > 1 ? Math.log(wordCount) / Math.log(largestWordCount) *
                (maxAlphaVal - minAlphaVal) + minAlphaVal : minAlphaVal);
    }

    public void setDefaultWordColor(int defaultWordColor) {
        this.defaultWordColor = defaultWordColor;
    }

    public void setDimenWidth(int dimenWidth) {
        this.dimenWidth = dimenWidth;
    }

    public void setDimenHeight(int dimenHeight) {
        this.dimenHeight = dimenHeight;
    }

    public void setDefaultBackgroundColor(int defaultBackgroundColor) {
        this.defaultBackgroundColor = defaultBackgroundColor;
    }

    public void setMaxFontSize(int maxFontSize) {
        this.maxFontSize = maxFontSize;
    }

    public void setMinFontSize(int minFontSize) {
        this.minFontSize = minFontSize;
    }

    public void setMaxColorAlphaValue(int maxColorAlphaValue) {
        this.maxColorAlphaValue = maxColorAlphaValue;
    }

    public void setMinColorAlphaValue(int minColorAlphaValue) {
        this.minColorAlphaValue = minColorAlphaValue;
    }

    public void setWordColorOpacityAuto(boolean wordColorOpacityAuto) {
        this.wordColorOpacityAuto = wordColorOpacityAuto;
    }

    public void setCustomTypeFace(Typeface customTypeFace) {
        this.customTypeFace = customTypeFace;
    }

    public void setPaddingX(int paddingX) {
        this.paddingX = paddingX;
    }

    public void setPaddingY(int paddingY) {
        this.paddingY = paddingY;
    }


    public void setBoundingYAxis(boolean boundingYAxis) {
        this.boundingYAxis = boundingYAxis;
    }
}
