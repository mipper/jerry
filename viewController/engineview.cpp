#include <QTextEdit>
#include "engineview.h"
#include "uci/engine_info.h"
#include <QDebug>


EngineView::EngineView(GameModel *gameModel, QWidget *parent) :
    QTextEdit(parent)
{
    this->lastInfo = QString("");
    this->gameModel = gameModel;
}

void EngineView::onNewInfo(QString info) {
    this->lastInfo = info;
    if(this->gameModel->showEval) {
        this->setText(info);
    }
}

void EngineView::flipShowEval() {

    this->gameModel->showEval = !this->gameModel->showEval;
    this->setText("");
    this->onNewInfo(this->lastInfo);
}