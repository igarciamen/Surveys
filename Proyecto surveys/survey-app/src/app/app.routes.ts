import { Routes } from '@angular/router';

import { LoginComponent } from './components/login-component/login-component';
import { SignupComponent } from './components/signup-component/signup-component';
import { SurveyListComponent } from './components/survey-list-component/survey-list-component';
import { SurveyDetailComponent } from './components/survey-detail-component/survey-detail-component';
import { MySurveysComponent } from './components/my-surveys-component/my-surveys-component';
import { SurveyCreateComponent } from './components/survey-create-component/survey-create-component';
import { SurveyAnswerComponent } from './components/survey-answer-component/survey-answer-component';
import { SurveyResultsComponent } from './components/survey-results-component/survey-results-component';
import { AuthGuard } from './guards/auth-guard';
import { SurveyStatisticsComponent } from './components/survey-statistics-component/survey-statistics-component';
import { SurveyInvitationsComponent } from './components/survey-invitations-component/survey-invitations-component';

export const routes: Routes = [
  { path: '', redirectTo: 'surveys', pathMatch: 'full' },

  // Auth
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },

  // Designer area (specific routes BEFORE the /:id wildcard).
  {
    path: 'surveys/new',
    component: SurveyCreateComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_DESIGNER', 'ROLE_ADMIN'] },
  },
  {
    path: 'surveys/manage',
    component: MySurveysComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_DESIGNER', 'ROLE_ADMIN'] },
  },
  {
    path: 'surveys/:id/edit',
    component: SurveyCreateComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_DESIGNER', 'ROLE_ADMIN'] },
  },

  // Answering is public; the access mode (OPEN / PASSWORD / RESTRICTED) is enforced
  // by the component and the backend, so RESTRICTED still requires logging in.
  {
    path: 'surveys/:id/answer',
    component: SurveyAnswerComponent,
  },
  // Results: only the owner (designer) or an admin.
  {
    path: 'surveys/:id/results',
    component: SurveyResultsComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_DESIGNER', 'ROLE_ADMIN'] },
  },

  {
  path: 'surveys/:id/statistics',
  component: SurveyStatisticsComponent,
  canActivate: [AuthGuard],
  data: { roles: ['ROLE_DESIGNER', 'ROLE_ADMIN'] },
},

{
  path: 'surveys/:id/invitations',
  component: SurveyInvitationsComponent,
  canActivate: [AuthGuard],
  data: { roles: ['ROLE_DESIGNER', 'ROLE_ADMIN'] },
},

  // Public catalog
  { path: 'surveys', component: SurveyListComponent },
  { path: 'surveys/:id', component: SurveyDetailComponent },

  { path: '**', redirectTo: 'surveys' },

  

];
