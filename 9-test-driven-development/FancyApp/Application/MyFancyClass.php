<?php
/**
 * Created by PhpStorm.
 * User: s.kemper
 * Date: 29.03.2022
 * Time: 22:05
 *
 * This class does a lot of things... but has no productive purpose.
 * The only purpose of this class is to teach the basic functions
 * of PHP and how to use them.
 */

class MyFancyClass
{
    /**
     * Shortens $text to at most $length characters, $ending included.
     * Returns false if $ending alone is longer than $length.
     */
    public function shortString($text, $length, $ending = '...') {
        // mb_*: count characters, not bytes ("Länge" = 5 characters, 6 bytes)
        if (mb_strlen($ending) > $length) {
            return false;
        }
        if (mb_strlen($text) <= $length) {
            return $text;
        }
        return mb_substr($text, 0, $length - mb_strlen($ending)) . $ending;
    }

    /**
     * Average of all values; 0 for an empty list.
     * Returns false as soon as one value is not numeric.
     */
    public function calcAverage($values) {
        if (count($values) === 0) {
            return 0;
        }
        foreach ($values as $value) {
            if (!is_numeric($value)) {
                return false;
            }
        }
        return array_sum($values) / count($values);
    }

    /**
     * String -> array (explode), array -> string (implode).
     * Returns false for every other type and for an empty delimiter.
     */
    public function getOpposite($value, $delimiter = ',') {
        if ($delimiter === '') {
            return false;
        }
        if (is_string($value)) {
            return explode($delimiter, $value);
        }
        if (is_array($value)) {
            return implode($delimiter, $value);
        }
        return false;
    }
}
