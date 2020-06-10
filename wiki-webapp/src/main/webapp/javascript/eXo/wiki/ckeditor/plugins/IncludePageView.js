import View from '@ckeditor/ckeditor5-ui/src/view';

import ButtonView from '@ckeditor/ckeditor5-ui/src/button/buttonview';
import LabeledInputView from '@ckeditor/ckeditor5-ui/src/labeledinput/labeledinputview';
import InputTextView from '@ckeditor/ckeditor5-ui/src/inputtext/inputtextview';

import submitHandler from '@ckeditor/ckeditor5-ui/src/bindings/submithandler';

import checkIcon from '@ckeditor/ckeditor5-core/theme/icons/check.svg';
import cancelIcon from '@ckeditor/ckeditor5-core/theme/icons/cancel.svg';

/**
 * The include page form view controller class.
 *
 * @extends module:ui/view~View
 */
export default class IncludePageFormView extends View {
  /**
	 * @inheritDoc
	 */
  constructor(locale) {
    super(locale);

    const t = locale.t;

    /**
		 * The URL input view.
		 *
		 * @member {module:ui/labeledinput/labeledinputview~LabeledInputView}
		 */
    this.pageNameInputView = this._createPageNameInput();

    /**
		 * The Save button view.
		 *
		 * @member {module:ui/button/buttonview~ButtonView}
		 */
    this.saveButtonView = this._createButton(t('Save'), checkIcon, 'ck-button-save');
    this.saveButtonView.type = 'submit';

    /**
		 * The Cancel button view.
		 *
		 * @member {module:ui/button/buttonview~ButtonView}
		 */
    this.cancelButtonView = this._createButton(t('Cancel'), cancelIcon, 'ck-button-cancel', 'cancel');

    this.setTemplate({
      tag: 'form',

      attributes: {
        class: [
          'ck',
          'ck-link-form',
        ]
      },

      children: [
        this.pageNameInputView,
        this.saveButtonView,
        this.cancelButtonView
      ]
    });
  }

  /**
	 * @inheritDoc
	 */
  render() {
    super.render();

    submitHandler({
      view: this
    });
  }

  /**
	 * Creates a labeled input view.
	 *
	 * @private
	 * @returns {module:ui/labeledinput/labeledinputview~LabeledInputView} Labeled input view instance.
	 */
  _createPageNameInput() {
    const t = this.locale.t;

    const labeledInput = new LabeledInputView(this.locale, InputTextView);

    labeledInput.label = t('Page Name');
    labeledInput.inputView.placeholder = 'My Page';

    return labeledInput;
  }

  /**
	 * Creates a button view.
	 *
	 * @private
	 * @param {String} label The button label.
	 * @param {String} icon The button's icon.
	 * @param {String} className The additional button CSS class name.
	 * @param {String} [eventName] An event name that the `ButtonView#execute` event will be delegated to.
	 * @returns {module:ui/button/buttonview~ButtonView} The button view instance.
	 */
  _createButton(label, icon, className, eventName) {
    const button = new ButtonView(this.locale);

    button.set({
      label,
      icon,
      tooltip: true
    });

    button.extendTemplate({
      attributes: {
        class: className
      }
    });

    if (eventName) {
      button.delegate('execute').to(this, eventName);
    }

    return button;
  }
}