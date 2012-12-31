/*
 * Accordionza jQuery Plugin
 * Copyright 2010, Geert De Deckere <geert@idoe.be>
 */
(function($) {

	// Accordionza version
	var version = '1.0.2';

	$.fn.accordionza = function(options) {

		// Merge all the options (recursively)
		var o = $.extend(true, {}, $.fn.accordionza.defaults, options);

		// Loop over each accordion
		this.each(function() {

			// Cache selector operations
			var $accordion = $(this);
			var $slides = $accordion.children('li');
			var $firstSlide = $slides.filter(':first');
			var $lastSlide = $slides.filter(':last');
			var $activeSlide = $slides.filter('.' + o.classSlideOpened);

			// Calculate widths and heights
			var width = $accordion.width();
			var height = $accordion.height();
			var slideMinWidth = (o.slideWidthClosed === false) ? $slides.filter(':not(.' + o.classSlideOpened + '):first').find('.' + o.classHandle + ':first').outerWidth() : o.slideWidthClosed;
			var slideMaxWidth = width - ($slides.length - 1) * slideMinWidth;

			// Handlers for setInterval and setTimeout
			var autoPlayInterval;
			var autoPlayTimeout;

			// Used for o.pauseOnHover functionality
			var autoPlayPaused = false;

			// Setup the accordion (wrapper)
			$accordion.css({
				'position': 'relative',
				'overflow': 'hidden'
			});

			// Setup the slides
			$slides.css({
				'position': 'absolute',
				'top': '0',
				'width': slideMaxWidth + 'px',
				'height': height + 'px'
			});

			// Position each slide
			$slides.each(function(i) {
				// We also store the order for each slide (starting with zero)
				// This will come in handy later
				$(this).css('left', i * slideMinWidth + 'px').data('order', i);
			});

			// Hide the captions
			$('.' + o.classCaption, $slides).css('top', height + 'px');

			// Start autoplaying
			function startAutoPlay() {
				// Make sure we don't start a double interval
				stopAutoPlay();

				autoPlayInterval = setInterval(function() {
					// If not temporarily paused, trigger the next slide
					if ( ! autoPlayPaused) {
						nextSlide().trigger('slide');
					}
				}, o.slideDelay);
			}

			// Stop autoplaying
			function stopAutoPlay() {
				clearInterval(autoPlayInterval);
			}

			// Pause autoplaying for a while
			function pauseAutoPlay(delay) {
				// Set the default delay value
				if (delay === undefined) {
					var delay = o.autoRestartDelay;
				}

				stopAutoPlay();

				// If the delay has been set to false (or 0),
				// we won't restart autoplaying the slides.
				if (delay === false) {
					return;
				}

				// Clear any possible previous calls to this function
				clearTimeout(autoPlayTimeout);

				// Note: on top of the delay value below, you have to add the default o.slideDelay time
				// to get the actual time the next slide gets triggered. This is because setInterval() in
				// startAutoPlay() waits one delay iteration before starting.
				autoPlayTimeout = setTimeout(function() {
					startAutoPlay();
				}, delay);
			}

			// Returns the previous slide
			function prevSlide(loop) {
				if (loop === undefined) {
					var loop = true;
				}
				var $prevSlide = $activeSlide.prev();
				if ($prevSlide.length) {
					return $prevSlide;
				} else if (loop) {
					return $lastSlide;
				} else {
					return $activeSlide;
				}
			}

			// Returns the next slide
			function nextSlide(loop) {
				if (loop === undefined) {
					var loop = true;
				}
				var $nextSlide = $activeSlide.next();
				if ($nextSlide.length) {
					return $nextSlide;
				} else if (loop) {
					return $firstSlide;
				} else {
					return $activeSlide;
				}
			}

			// Some slide was activated by the user
			$slides.bind(o.slideTrigger, function() {
				pauseAutoPlay();
				$(this).trigger('slide');
			});

			// Some slide was activated
			$slides.bind('slide', function() {
				// Ignore clicks on an already opened slide
				if ($(this).hasClass(o.classSlideOpened)) {
					return;
				}

				// Switch the classSlideOpened
				$activeSlide.removeClass(o.classSlideOpened);
				$(this).addClass(o.classSlideOpened);

				// Move all following slides to the right, and all preceding slides to the left
				$slides.filter(':gt(' + $(this).data('order') + ')').each(function() {
					$(this).stop(true).animate({left: slideMaxWidth + ($(this).data('order') - 1) * slideMinWidth + 'px'}, o.slideSpeed, o.slideEasing);
				}).end().filter(':lt(' + ($(this).data('order') + 1) + ')').each(function() {
					$(this).stop(true).animate({left: $(this).data('order') * slideMinWidth + 'px'}, o.slideSpeed, o.slideEasing);
				});

				// Hide the old caption, show the new one
				$('.' + o.classCaption, $activeSlide).stop(true).animate({top: height + 'px'}, o.captionSpeed, o.captionEasing);
				// Note: using delay() here from jQuery 1.4 instead of the "animate({opacity:0},delay)" trick from http://www.learningjquery.com/2007/01/effect-delay-trick
				//       Using opacity here would cause PNG transparency to break in IE
				$('.' + o.classCaption, $(this)).stop(true).delay(o.captionDelay).animate({top: height - o.captionHeight + 'px'}, o.captionSpeed, o.captionEasing);

				// User-defined callbacks
				if ($.isFunction(o.onSlideClose)) {
					o.onSlideClose.call($activeSlide);
				}
				if ($.isFunction(o.onSlideOpen)) {
					o.onSlideOpen.call($(this));
				}

				// Custom event hook
				$.event.trigger('accordionza_slide');

				// Voilà, we've got a new open slide
				$activeSlide = $(this);
			});

			// Animate and toggle the caption
			$slides.find('.' + o.classCaptionToggle).click(function() {
				pauseAutoPlay();

				// Find the caption this classCaptionToggle instance belongs to
				// Note: we don't assume classCaptionToggle is an ancestor of the caption
				var $caption = $(this).closest('li').find('.' + o.classCaption);

				if ( ! $activeSlide.hasClass(o.classCaptionCollapsed)) {
					// Move the caption down
					$caption.stop(true).animate({top: height - o.captionHeightClosed + 'px'}, o.captionSpeed, o.captionEasing);
				} else {
					// Move the caption back up
					$caption.stop(true).animate({top: height - o.captionHeight + 'px'}, o.captionSpeed, o.captionEasing);
				}

				$activeSlide.toggleClass(o.classCaptionCollapsed);
			});

			// Keyboard navigation
			if (o.navKey) {
				$(document.documentElement).keyup(function(event) {
					// Left arrow key
					if (event.which == 37) {
						prevSlide().trigger(o.slideTrigger);
					// Right arrow key
					} else if (event.which == 39) {
						nextSlide().trigger(o.slideTrigger);
					}
				});
			}

			// Open up the start slide.
			// Note: this code needs to come after the 'slide' binding to $slides.
			if ($activeSlide.length) {
				// If a slide with the classSlideOpened exists,
				// we trigger the previous one so the correct slide opens up.
				prevSlide().trigger('slide');
			} else {
				// If no slide with the classSlideOpened is found,
				// we open up the first slide.
				$lastSlide.addClass(o.classSlideOpened);
				$activeSlide = $lastSlide;
				$firstSlide.trigger('slide');
			}

			// Start autoplaying
			if (o.autoPlay) {
				// Temporarily pause the slideshow on hover
				if (o.pauseOnHover) {
					$accordion.mouseenter(function() {
						autoPlayPaused = true;
					}).mouseleave(function() {
						autoPlayPaused = false;
					});
				}

				startAutoPlay();
			}
		});

		// Make chainable
		return this;
	};

	// The complete list of default options
	$.fn.accordionza.defaults = {
		autoPlay:              false,
		autoRestartDelay:      false,
		captionDelay:          0,
		captionEasing:         'swing',
		captionHeight:         50,
		captionHeightClosed:   0,
		captionSpeed:          500,
		classSlideOpened:      'slide_opened',
		classCaption:          'slide_caption',
		classCaptionCollapsed: 'slide_caption_collapsed',
		classCaptionToggle:    'slide_caption_toggle',
		classHandle:           'slide_handle',
		navKey:                false,
		onSlideClose:          null,
		onSlideOpen:           null,
		pauseOnHover:          false,
		slideDelay:            5000,
		slideEasing:           'swing',
		slideSpeed:            500,
		slideTrigger:          'click',
		slideWidthClosed:      false
	};

})(jQuery);