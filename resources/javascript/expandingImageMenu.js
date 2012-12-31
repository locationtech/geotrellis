			$(function() {
				var $menu				= $('#ei_menu > ul'),
					$menuItems			= $menu.children('li'),
					$menuItemsImgWrapper= $menuItems.children('a'),
					$menuItemsPreview	= $menuItemsImgWrapper.children('.ei_preview'),
					totalMenuItems		= $menuItems.length,
				
					ExpandingMenu 	= (function(){
						/*
							@current
							set it to the index of the element you want to be opened by default,
							or -1 if you want the menu to be closed initially
						 */
						var current				= 3,
						/*
							@anim
							if we want the default opened item to animate initialy set this to true
						 */
						anim				= true,
						/*
							checks if the current value is valid -
							between 0 and the number of items
						 */
						validCurrent		= function() {
							return (current >= 0 && current < totalMenuItems);
						},
						init				= function() {
								/* show default item if current is set to a valid index */
							if(validCurrent())
								configureMenu();

							initEventsHandler();
						},
						configureMenu		= function() {
								/* get the item for the current */
							var $item	= $menuItems.eq(current);
								/* if anim is true slide out the item */
							if(anim)
								slideOutItem($item, true, 900, 'easeInQuint');
							else{
									/* if not just show it */
								$item.css({width : '400px'})
								.find('.ei_image')
								.css({left:'0px', opacity:1});

									/* decrease the opacity of the others */
									$menuItems.not($item)
											  .children('.ei_preview')
											  .css({opacity:0.2});
							}
						},
						initEventsHandler	= function() {
								/*
								when we click an item the following can happen:
								1) The item is already opened - close it!
								2) The item is closed - open it! (if another one is opened, close it!)
								*/
							$menuItemsImgWrapper.bind('click.ExpandingMenu', function(e) {
								var $this 	= $(this).parent(),
								idx		= $this.index();

								if(current === idx) {
									slideOutItem($menuItems.eq(current), false, 1500, 'easeOutQuint', true);
									current = -1;
								}
								else{
									if(validCurrent() && current !== idx)
											slideOutItem($menuItems.eq(current), false, 250, 'jswing');

									current	= idx;
										slideOutItem($this, true, 250, 'jswing');
								}
								return false;
							});
						},
							/* if you want to trigger the action to open a specific item */
							openItem			= function(idx) {
								$menuItemsImgWrapper.eq(idx).click();
							},
							/*
							opens or closes an item
							note that "mLeave" is just true when all the items close,
							in which case we want that all of them get opacity 1 again.
							"dir" tells us if we are opening or closing an item (true | false)
							*/
						slideOutItem		= function($item, dir, speed, easing, mLeave) {
							var $ei_image	= $item.find('.ei_image'),

							itemParam	= (dir) ? {width : '400px'} : {width : '75px'},
							imageParam	= (dir) ? {left : '0px'} : {left : '75px'};

								/*
								if opening, we animate the opacity of all the elements to 0.1.
								this is to give focus on the opened item..
								*/
							if(dir)
							/*
									alternative:
									$menuItemsPreview.not($menuItemsPreview.eq(current))
													 .stop()
													 .animate({opacity:0.1}, 500);
							 */
								$menuItemsPreview.stop()
							.animate({opacity:0.1}, 1000);
							else if(mLeave)
								$menuItemsPreview.stop()
							.animate({opacity:1}, 1500);

								/* the <li> expands or collapses */
							$item.stop().animate(itemParam, speed, easing);
								/* the image (color) slides in or out */
							$ei_image.stop().animate(imageParam, speed, easing, function() {
									/*
									if opening, we animate the opacity to 1,
									otherwise we reset it.
									*/
								if(dir)
									$ei_image.animate({opacity:1}, 2000);
								else
									$ei_image.css('opacity', 0.2);
							});
						};

						return {
							init 		: init,
							openItem	: openItem
						};
					})();
					
				/*
				call the init method of ExpandingMenu
				*/
				ExpandingMenu.init();
			
			/*
			if later on you want to open / close a specific item you could do it like so:
			ExpandingMenu.openItem(3); // toggles item 3 (zero-based indexing)
			*/
			});