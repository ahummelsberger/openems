import { ActivatedRoute } from '@angular/router';
import { AsymmetricPeakshavingModalComponent } from './modal/modal.component';
import { Component, Input, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Edge, Service, EdgeConfig } from 'src/app/shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: AsymmetricPeakshavingWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class AsymmetricPeakshavingWidgetComponent implements OnInit {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() private componentId: string;

    private static readonly SELECTOR = "asymmetricPeakshavingWidget";

    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: AsymmetricPeakshavingModalComponent,
            cssClass: 'wide-modal',
            componentProps: {
                component: this.component
            }
        });
        return await modal.present();
    }
}