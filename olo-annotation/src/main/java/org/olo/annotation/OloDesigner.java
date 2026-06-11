package org.olo.annotation;



import java.lang.annotation.Documented;

import java.lang.annotation.Retention;

import java.lang.annotation.RetentionPolicy;

import java.lang.annotation.Target;



import static java.lang.annotation.ElementType.ANNOTATION_TYPE;



/**

 * Studio designer metadata — palette grouping, search, and canvas rendering hints.

 * Nested in {@link OloNode}, {@link OloTool}, {@link OloHook}, and {@link OloWorkflowPreset}.

 */

@Documented

@Target(ANNOTATION_TYPE)

@Retention(RetentionPolicy.CLASS)

public @interface OloDesigner {



    /** Palette section label (for example {@code Agents}). Falls back to {@code category} when blank. */

    String paletteGroup() default "";



    /** Extra search terms beyond {@code tags}. */

    String[] searchKeywords() default {};



    /** Canvas width in pixels ({@code designer.nodeSize.width}). {@code 0} uses {@link #canvasShape()} hint. */

    int width() default 0;



    /** Canvas height in pixels ({@code designer.nodeSize.height}). {@code 0} uses {@link #canvasShape()} hint. */

    int height() default 0;



    boolean resizable() default true;



    boolean draggable() default true;



    /** Canvas shape hint when {@link #width()} / {@link #height()} are unset. */

    OloNodeShape canvasShape() default OloNodeShape.STANDARD;

}


