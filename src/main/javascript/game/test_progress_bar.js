import {objects, ProgressBar, PushSocket} from '../main';


class TestProgressBar extends ProgressBar {
    constructor(progressElement, gameId) {
        super(progressElement);

        /**
         * Game ID of the current game.
         * @type {number}
         */
        this.gameId = gameId;
    }

    async initAsync () {
        this.pushSocket = await objects.await('pushSocket');
    }

    async activate () {
        this.setProgress(16, 'Submitting Test');
        await this._register();
        await this._subscribe();

        /* Reconnect on close, because on Firefox the WebSocket connection gets closed on POST. */
        const reconnect = event => {
            this.pushSocket.unregister(PushSocket.WSEventType.CLOSE, reconnect);
            this.pushSocket.reconnect();
            this._subscribe();
        };
        this.pushSocket.register(PushSocket.WSEventType.CLOSE, reconnect);
    }

    async _subscribe () {
        this.pushSocket.subscribe('registration.TestProgressBarRegistrationEvent', {
            gameId: this.gameId
        });
    }

    async _register () {
        this.pushSocket.register('test.TestSubmittedEvent', this._onTestSubmitted.bind(this));
        this.pushSocket.register('test.TestCompiledEvent', this._onTestCompiled.bind(this));
        this.pushSocket.register('test.TestValidatedEvent', this._onTestValidated.bind(this));
        this.pushSocket.register('test.TestTestedOriginalEvent', this._onTestTestedOriginal.bind(this));
        this.pushSocket.register('test.TestTestedMutantsEvent', this._onTestTestedMutants.bind(this));
    }

    _onTestSubmitted (event) {
        this.setProgress(33, 'Validating Test');
    }

    _onTestValidated (event) {
        if (event.success) {
            this.setProgress(50, 'Compiling Test');
        } else {
            this.setProgress(100, 'Test Is Not Valid');
        }
    }

    _onTestCompiled (event) {
        if (event.success) {
            this.setProgress(66, 'Running Test Against Original');
        } else {
            this.setProgress(100, 'Test Did Not Compile');
        }
    }

    _onTestTestedOriginal (event) {
        if (event.success) {
            this.setProgress(83, 'Running Test Against Mutants');
        } else {
            this.setProgress(100, 'Test Failed Against Original');
        }
    }

    _onTestTestedMutants (event) {
        this.setProgress(100, 'Done');
    }
}


export default TestProgressBar;
